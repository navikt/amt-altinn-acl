package no.nav.amt.altinn.acl.client.maskinporten

import no.nav.amt.altinn.acl.client.altinn.ALTINN3_CLIENT_ID
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.endpoint.RestClientJwtBearerTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

/**
 * Kobler Maskinportens JWT-bearer-grant til Spring Security sin OAuth2-klientflyt.
 *
 * Maskinporten bruker en signert JWT-assertion i stedet for vanlig
 * `client_secret`-autentisering eller `client_credentials`. Derfor registreres
 * klienten og grant-provideren eksplisitt her.
 */
@Configuration(proxyBeanMethods = false)
class MaskinportenClientConfig {
    @Value($$"${altinn3.url}")
    lateinit var altinn3Url: String

    @Value($$"${maskinporten.scopes}")
    lateinit var maskinportenScopes: String

    @Value($$"${maskinporten.client-id}")
    lateinit var maskinportenClientId: String

    @Value($$"${maskinporten.issuer}")
    lateinit var maskinportenIssuer: String

    @Value($$"${maskinporten.token-endpoint}")
    lateinit var maskinportenTokenEndpoint: String

    @Value($$"${maskinporten.client-jwk}")
    lateinit var maskinportenClientJwk: String

    /**
     * Registrerer Altinn-klienten med Maskinportens JWT-bearer-grant.
     *
     * Standardregistreringen for `client_credentials` kan ikke brukes fordi
     * Maskinporten krever en signert assertion. `ClientAuthenticationMethod.NONE`
     * betyr at klienten ikke autentiserer seg med client secret; assertionen
     * sendes som del av grant-forespørselen.
     */
    @Bean
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        val registration = ClientRegistration
            .withRegistrationId(ALTINN3_CLIENT_ID)
            .clientId(maskinportenClientId)
            .tokenUri(maskinportenTokenEndpoint)
            .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .scope(*maskinportenScopes.split(" ").toTypedArray())
            .build()

        return InMemoryClientRegistrationRepository(registration)
    }

    /**
     * Oppretter signeringskomponenten som lager en kortlivet assertion for hver
     * tokenforespørsel. Assertionen identifiserer klienten overfor Maskinporten
     * og angir Altinn 3 som ressursen tokenet skal brukes mot.
     */
    @Bean
    fun maskinportenJwtAssertionBuilder(): MaskinportenJwtAssertionBuilder = MaskinportenJwtAssertionBuilder(
        clientId = maskinportenClientId,
        issuer = maskinportenIssuer,
        resource = altinn3Url,
        scopes = maskinportenScopes.split(" "),
        privateJwk = maskinportenClientJwk,
    )

    /**
     * Konfigurerer Maskinportens JWT-bearer-grant i Spring Security.
     *
     * Spring sin [OAuth2AuthorizedClientManager] håndterer caching og fornyelse av token.
     * Manageren pakkes med en fast principal fordi Maskinporten-tokenet tilhører applikasjonen,
     * ikke brukeren eller tjenesten som utløste Altinn-kallet.
     *
     * Spring sin JWT-bearer-provider utfører tokenutvekslingen og håndterer utløp.
     * Den egendefinerte assertionbyggeren leverer Maskinporten-assertionen, og tokenkallet
     * bruker Boot-konfigurert `RestClient` for å dele timeout- og observability-oppsett.
     */
    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        maskinportenJwtAssertionBuilder: MaskinportenJwtAssertionBuilder,
        restClientBuilder: RestClient.Builder,
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder
            .builder()
            .provider(
                maskinportenJwtBearerProvider(
                    assertionBuilder = maskinportenJwtAssertionBuilder,
                    restClientBuilder = restClientBuilder,
                ),
            ).build()

        val delegate = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository),
        )
        delegate.setAuthorizedClientProvider(authorizedClientProvider)

        return fixedPrincipalManager(delegate)
    }
}

/**
 * Lager Spring sin innebygde JWT-bearer-provider med Maskinportens assertion og token-endepunkt.
 *
 * Klokkeslakk på ti sekunder viderefører fornyelsesmarginen fra den tidligere provideren.
 * Hvis tokenresponsen mangler en gyldig utløpstid, brukes samme standardlevetid som tidligere.
 * Feilsvar fra token-endepunktet redigeres før Spring bygger OAuth2-feilen, slik at rå
 * responsbody ikke blir del av feilmeldingen.
 */
internal fun maskinportenJwtBearerProvider(
    assertionBuilder: MaskinportenJwtAssertionBuilder,
    restClientBuilder: RestClient.Builder,
): JwtBearerOAuth2AuthorizedClientProvider {
    val restClientTokenResponseClient = RestClientJwtBearerTokenResponseClient().apply {
        setRestClient(
            restClientBuilder
                .clone()
                .configureMessageConverters { converters ->
                    converters.addCustomConverter(FormHttpMessageConverter())
                    converters.addCustomConverter(maskinportenTokenResponseConverter())
                }.defaultStatusHandler({ status -> status.isError }) { _, response ->
                    throw RestClientException("Klarte ikke hente Maskinporten-token code=${response.statusCode.value()}")
                }.build(),
        )
    }

    return JwtBearerOAuth2AuthorizedClientProvider().apply {
        setJwtAssertionResolver { assertionBuilder.buildJwt() }
        setAccessTokenResponseClient(restClientTokenResponseClient)
        setClockSkew(Duration.ofSeconds(10))
    }
}

/**
 * Beholder Spring sin standardtolking av tokenresponsen, men setter en avgrenset
 * standardlevetid når Maskinporten utelater `expires_in` eller oppgir en ugyldig verdi.
 */
private fun maskinportenTokenResponseConverter(): OAuth2AccessTokenResponseHttpMessageConverter {
    val defaultConverter = DefaultMapOAuth2AccessTokenResponseConverter()

    return OAuth2AccessTokenResponseHttpMessageConverter().apply {
        setAccessTokenResponseConverter { parameters ->
            val expiresIn = parameters["expires_in"]?.toString()?.toLongOrNull()
            if (expiresIn == null || expiresIn <= 0) {
                defaultConverter.convert(parameters + ("expires_in" to DEFAULT_TOKEN_LIFETIME_SECONDS))
            } else {
                defaultConverter.convert(parameters)
            }
        }
    }
}

private const val DEFAULT_TOKEN_LIFETIME_SECONDS = 120L

/**
 * Erstatter innkommende principal med en stabil applikasjonsprincipal.
 *
 * [AuthorizedClientServiceOAuth2AuthorizedClientManager] cacher token per kombinasjon av
 * klientregistrering og principalnavn. En fast principal gir derfor én delt Maskinporten-cache
 * for både HTTP-kall og bakgrunnsjobber, i stedet for én cacheoppføring per innkommende klient.
 */
internal fun fixedPrincipalManager(delegate: OAuth2AuthorizedClientManager): OAuth2AuthorizedClientManager =
    OAuth2AuthorizedClientManager { request ->
        delegate.authorize(
            OAuth2AuthorizeRequest
                .withClientRegistrationId(request.clientRegistrationId)
                .principal(MASKINPORTEN_PRINCIPAL)
                .attributes { it.putAll(request.attributes) }
                .build(),
        )
    }

private const val MASKINPORTEN_PRINCIPAL = "maskinporten-system"

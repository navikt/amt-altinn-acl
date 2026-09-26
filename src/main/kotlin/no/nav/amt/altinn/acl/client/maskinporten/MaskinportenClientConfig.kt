package no.nav.amt.altinn.acl.client.maskinporten

import no.nav.amt.altinn.acl.client.altinn.ALTINN3_CLIENT_ID
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.web.client.RestClient

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

    // Maskinporten sitt jwt-bearer-grant med selvsignert assertion er ikke et standard
    // OAuth2 client_credentials-flow, så vi registrerer klienten selv i stedet for å
    // bruke spring.security.oauth2.client.registration.* auto-konfigurasjon.
    @Bean
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        val registration = ClientRegistration
            .withRegistrationId(ALTINN3_CLIENT_ID)
            .clientId(maskinportenClientId)
            .tokenUri(maskinportenTokenEndpoint)
            .authorizationGrantType(MASKINPORTEN_JWT_BEARER_GRANT_TYPE)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .scope(*maskinportenScopes.split(" ").toTypedArray())
            .build()

        return InMemoryClientRegistrationRepository(registration)
    }

    @Bean
    fun maskinportenJwtAssertionBuilder(): MaskinportenJwtAssertionBuilder = MaskinportenJwtAssertionBuilder(
        clientId = maskinportenClientId,
        issuer = maskinportenIssuer,
        resource = altinn3Url,
        scopes = maskinportenScopes.split(" "),
        privateJwk = maskinportenClientJwk,
    )

    /**
     * Konfigurerer Maskinporten som et egendefinert JWT-bearer-grant i Spring Security.
     *
     * Spring sin [OAuth2AuthorizedClientManager] håndterer caching og fornyelse av token.
     * Manageren pakkes med en fast principal fordi Maskinporten-tokenet tilhører applikasjonen,
     * ikke brukeren eller tjenesten som utløste Altinn-kallet.
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
                MaskinportenAuthorizedClientProvider(
                    maskinportenJwtAssertionBuilder,
                    restClientBuilder.build(),
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

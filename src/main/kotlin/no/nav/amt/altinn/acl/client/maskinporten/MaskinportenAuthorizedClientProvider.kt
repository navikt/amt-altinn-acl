package no.nav.amt.altinn.acl.client.maskinporten

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.requiredBody
import java.time.Instant

val MASKINPORTEN_JWT_BEARER_GRANT_TYPE = AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer")

/**
 * Registrert i Spring sin OAuth2AuthorizedClientManager slik at
 * `@ClientRegistrationId("altinn3")` på AltinnApi fungerer på samme måte som for
 * de vanlige (client_credentials) klientene i denne kodebasen.
 *
 * Caching/gjenbruk av token gjøres av Spring (via OAuth2AuthorizedClientService) - denne
 * klassen kalles kun når Spring mangler et gyldig token og trenger et nytt. Vi returnerer
 * `null` når eksisterende token fortsatt er gyldig, slik kontrakten til
 * OAuth2AuthorizedClientProvider krever.
 *
 * Token-requesten sendes med applikasjonens Boot-konfigurerte [RestClient], slik at
 * Maskinporten-kallet bruker samme timeout, observability og HTTP-oppsett som øvrige klienter.
 * Nimbus brukes bare til å bygge og signere JWT-assertionen.
 *
 * NB: i motsetning til det gamle "******"-buggen, skal en feilet token-utveksling her
 * ALLTID kaste en exception (aldri returnere et tomt/ugyldig token stille).
 */
class MaskinportenAuthorizedClientProvider(
    private val assertionBuilder: MaskinportenJwtAssertionBuilder,
    private val restClient: RestClient,
) : OAuth2AuthorizedClientProvider {
    override fun authorize(context: OAuth2AuthorizationContext): OAuth2AuthorizedClient? {
        // En provider skal returnere null for grant-typer den ikke støtter, slik at
        // Spring Security kan prøve neste provider i kjeden.
        if (context.clientRegistration.authorizationGrantType != MASKINPORTEN_JWT_BEARER_GRANT_TYPE) {
            return null
        }

        val existing = context.authorizedClient
        // Null betyr her at ingen ny autorisering er nødvendig; manageren beholder tokenet.
        if (existing != null && !isExpired(existing)) {
            return null
        }

        return hentNyttToken(context)
    }

    private fun hentNyttToken(context: OAuth2AuthorizationContext): OAuth2AuthorizedClient {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", MASKINPORTEN_JWT_BEARER_GRANT_TYPE.value)
            add("scope", context.clientRegistration.scopes.joinToString(" "))
            add("assertion", assertionBuilder.build().serialize())
        }

        val response = try {
            restClient
                .post()
                .uri(context.clientRegistration.providerDetails.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .requiredBody<MaskinportenTokenResponse>()
        } catch (e: RestClientResponseException) {
            // RestClientResponseException beholdes ikke som cause fordi den kan inneholde rå responsbody.
            throw RuntimeException("Klarte ikke hente Maskinporten-token code=${e.statusCode.value()}")
        }

        val issuedAt = Instant.now()
        val lifetimeSeconds = response.expiresIn?.takeIf { it > 0 } ?: DEFAULT_LIFETIME_SECONDS

        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            response.accessToken,
            issuedAt,
            issuedAt.plusSeconds(lifetimeSeconds),
        )

        return OAuth2AuthorizedClient(
            context.clientRegistration,
            context.principal.name,
            accessToken,
        )
    }

    private fun isExpired(authorizedClient: OAuth2AuthorizedClient): Boolean {
        val expiresAt = authorizedClient.accessToken.expiresAt ?: return true
        return Instant.now().isAfter(expiresAt.minusSeconds(EXPIRY_SKEW_SECONDS))
    }

    companion object {
        private const val DEFAULT_LIFETIME_SECONDS = 120L
        private const val EXPIRY_SKEW_SECONDS = 10L
    }
}

/**
 * Feltene fra en vellykket Maskinporten-tokenrespons som provideren trenger for å
 * opprette Spring Security sitt [OAuth2AccessToken].
 */
private data class MaskinportenTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long?,
)

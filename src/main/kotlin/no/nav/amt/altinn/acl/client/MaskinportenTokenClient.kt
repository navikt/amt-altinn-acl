package no.nav.amt.altinn.acl.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.requiredBody
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * Henter Maskinporten-token fra Nais sitt lokale token-endepunkt.
 * Nais håndterer klientautentisering, signering, caching og fornyelse.
 */
@Service
class MaskinportenTokenClient(
    restClientBuilder: RestClient.Builder,
    // NAIS_TOKEN_ENDPOINT og ALTINN3_URL leses direkte fra env-variablene Nais setter, ikke via
    // egne property-navn i application.yml. Et navn som maskinporten.token-endpoint ville kollidert
    // med digdirator sin MASKINPORTEN_TOKEN_ENDPOINT via Spring sin relaxed binding, og stille
    // sendt token-kallet til Maskinporten i stedet for Texas.
    @Value($$"${NAIS_TOKEN_ENDPOINT}")
    private val maskinportenTokenEndpoint: String,
    // Settes i nais-manifestet. Vi ber bevisst om ett scope om gangen, og bruker derfor ikke
    // MASKINPORTEN_SCOPES - den inneholder alle scopes som er registrert på klienten.
    @Value($$"${ALTINN_SCOPE}")
    private val altinnScope: String,
    @Value($$"${ALTINN3_URL}")
    private val altinn3Url: String,
) {
    private val restClient = restClientBuilder.build()

    private val request: MultiValueMap<String, String> = CollectionUtils.unmodifiableMultiValueMap(
        CollectionUtils.toMultiValueMap(
            mapOf(
                "identity_provider" to listOf("maskinporten"),
                "target" to listOf(altinnScope),
                "resource" to listOf(altinn3Url),
            ),
        ),
    )

    fun getAccessToken(): String = try {
        restClient
            .post()
            .uri(maskinportenTokenEndpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(request)
            .retrieve()
            .requiredBody<MaskinportenTokenResponse>()
            .accessToken
    } catch (e: RestClientResponseException) {
        // Response body fra token-endepunktet inneholder aldri token ved feil, kun en OAuth-feilkode
        // (RFC 6749 §5.2). Vi plukker ut error/error_description og dropper resten av bodyen.
        throw MaskinportenTokenException(
            "Klarte ikke å hente Maskinporten-token code=${e.statusCode.value()} ${e.oauthError()}",
        )
    }

    private fun RestClientResponseException.oauthError(): String = runCatching {
        getResponseBodyAs(OAuthErrorResponse::class.java)
    }.getOrNull()
        ?.let { "error=${it.error} error_description=${it.errorDescription}" }
        ?: "error=<ukjent>"

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    private data class MaskinportenTokenResponse(
        val accessToken: String,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    private data class OAuthErrorResponse(
        val error: String? = null,
        val errorDescription: String? = null,
    )

    class MaskinportenTokenException(
        message: String,
    ) : RuntimeException(message)
}

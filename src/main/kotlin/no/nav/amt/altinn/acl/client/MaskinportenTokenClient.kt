package no.nav.amt.altinn.acl.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
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
    // Alle tre leses direkte fra env-variablene Nais setter, ikke via egne property-navn
    // i application.yml. Et navn som maskinporten.token-endpoint ville kollidert med
    // digdirator sin MASKINPORTEN_TOKEN_ENDPOINT via Spring sin relaxed binding, og
    // stille sendt token-kallet til Maskinporten i stedet for Texas.
    @Value($$"${NAIS_TOKEN_ENDPOINT}")
    private val maskinportenTokenEndpoint: String,
    @Value($$"${MASKINPORTEN_SCOPES}")
    private val maskinportenScopes: String,
    @Value($$"${ALTINN3_URL}")
    private val altinn3Url: String,
) {
    private val restClient = restClientBuilder.build()

    private val request = LinkedMultiValueMap<String, String>().apply {
        add("identity_provider", "maskinporten")
        add("target", maskinportenScopes)
        add("resource", altinn3Url)
    }

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

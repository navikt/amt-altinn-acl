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
    @Value($$"${maskinporten.token-endpoint}")
    private val maskinportenTokenEndpoint: String,
    @Value($$"${maskinporten.scopes}")
    private val maskinportenScopes: String,
    @Value($$"${altinn3.url}")
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
        // Unngå å logge response body fra token-endepunktet via exception cause.
        throw MaskinportenTokenException("Klarte ikke å hente Maskinporten-token code=${e.statusCode.value()}")
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    private data class MaskinportenTokenResponse(
        val accessToken: String,
    )

    class MaskinportenTokenException(
        message: String,
    ) : RuntimeException(message)
}

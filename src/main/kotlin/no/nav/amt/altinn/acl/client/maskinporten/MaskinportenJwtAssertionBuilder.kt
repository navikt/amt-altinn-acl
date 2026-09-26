package no.nav.amt.altinn.acl.client.maskinporten

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Bygger og signerer JWT-bearer-assertionen Maskinporten krever for
 * grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer.
 *
 * Claims:
 * - sub/iss = klientens client-id (Maskinporten identifiserer klienten på seg selv, ikke en bruker)
 * - aud = Maskinportens issuer (IKKE token-endpoint-URL-en - Maskinporten validerer aud mot issuer)
 * - resource = URL-en til APIet vi skal kalle (Altinn 3), et Maskinporten-spesifikt resource-indicator-krav
 * - exp = kort levetid (30s) - assertionen skal kun brukes momentant til å hente et faktisk access-token
 * - jti = unik per signering, hindrer replay av samme assertion
 */
class MaskinportenJwtAssertionBuilder(
    private val clientId: String,
    private val issuer: String,
    private val resource: String,
    private val scopes: List<String>,
    privateJwk: String,
) {
    private val privateJwkKeyId: String
    private val assertionSigner: JWSSigner

    init {
        val rsaKey = RSAKey.parse(privateJwk)
        privateJwkKeyId = rsaKey.keyID
        assertionSigner = RSASSASigner(rsaKey)
    }

    fun build(): SignedJWT = SignedJWT(assertionHeader(privateJwkKeyId), assertionClaims()).apply {
        sign(assertionSigner)
    }

    private fun assertionHeader(keyId: String): JWSHeader = JWSHeader.parse(
        mapOf(
            "kid" to keyId,
            "typ" to "JWT",
            "alg" to "RS256",
        ),
    )

    private fun assertionClaims(): JWTClaimsSet {
        val now = Instant.now()
        val expire = now.plusSeconds(ASSERTION_LIFETIME_SECONDS)

        return JWTClaimsSet
            .Builder()
            .subject(clientId)
            .audience(issuer)
            .issuer(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expire))
            .notBeforeTime(Date.from(now))
            .claim("scope", scopes.joinToString(" "))
            .claim("resource", resource)
            .jwtID(UUID.randomUUID().toString())
            .build()
    }

    companion object {
        private const val ASSERTION_LIFETIME_SECONDS = 30L
    }
}

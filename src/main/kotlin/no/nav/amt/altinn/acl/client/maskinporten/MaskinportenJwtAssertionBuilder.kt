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
 * Hver assertion signeres på nytt. Den er ikke access tokenet: den brukes kun
 * én gang som klientens bevis i forespørselen til Maskinporten, som returnerer
 * access tokenet Altinn-klienten senere bruker.
 *
 * Claims:
 * - sub/iss = klientens client-id (Maskinporten identifiserer klienten på seg selv, ikke en bruker)
 * - aud = Maskinportens issuer (IKKE token-endpoint-URL-en - Maskinporten validerer aud mot issuer)
 * - resource = URL-en til APIet vi skal kalle (Altinn 3), et Maskinporten-spesifikt resource-indicator-krav
 * - exp = kort levetid (30s) - assertionen skal kun brukes momentant til å hente et faktisk access-token
 * - jti = unik per signering, hindrer replay av samme assertion
 *
 * @param clientId Maskinporten-klientens ID, brukt som `iss` og `sub`.
 * @param issuer Maskinportens issuer, brukt som `aud` i assertionen.
 * @param resource URL-en til Altinn 3, som angir hvilket API tokenet skal gi tilgang til.
 * @param scopes Maskinporten-scope som legges i assertionen og tokenforespørselen.
 * @param privateJwk Privat RSA-JWK brukt til å signere assertionen. Nøkkelen forlater ikke applikasjonen.
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
        // Den private RSA-nøkkelen brukes lokalt til signering og sendes aldri til Maskinporten.
        val rsaKey = RSAKey.parse(privateJwk)
        privateJwkKeyId = rsaKey.keyID
        assertionSigner = RSASSASigner(rsaKey)
    }

    /**
     * Lager en ny assertion med ferske tidsstempler og unik `jti`, og signerer den
     * med klientens private RSA-nøkkel.
     */
    fun build(): SignedJWT = SignedJWT(assertionHeader(privateJwkKeyId), assertionClaims()).apply {
        sign(assertionSigner)
    }

    /**
     * Angir nøkkel-ID-en som Maskinporten bruker til å velge riktig offentlig nøkkel,
     * JWT-formatet og signeringsalgoritmen som hører til den private RSA-nøkkelen.
     */
    private fun assertionHeader(keyId: String): JWSHeader = JWSHeader.parse(
        mapOf(
            "kid" to keyId,
            "typ" to "JWT",
            "alg" to "RS256",
        ),
    )

    /**
     * Setter klientidentitet, Maskinporten som mottaker og Altinn 3 som ressurs.
     * `iat` og `nbf` settes til nå, mens `exp` begrenser assertionen til 30 sekunder.
     * En ny `jti` gjør hver assertion unik og hindrer gjenbruk av samme tokenforespørsel.
     */
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

package no.nav.amt.altinn.acl.client.maskinporten

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.altinn.acl.testutil.Constants.TEST_JWK
import org.junit.jupiter.api.Test

/**
 * Se MaskinportenAuthorizedClientProvider for den historiske bakgrunnen (regresjon mot
 * "******"-buggen i Authorization-header).
 */
class MaskinportenJwtAssertionBuilderTest {
    private val rsaKey = RSAKey.parse(TEST_JWK)

    private val builder = MaskinportenJwtAssertionBuilder(
        clientId = "client-id",
        issuer = "https://issuer.example",
        resource = "https://platform.tt02.altinn.no",
        scopes = listOf("scope1", "scope2"),
        privateJwk = TEST_JWK,
    )

    @Test
    fun `build - signerer JWT med riktige claims`() {
        val claims = builder.build().jwtClaimsSet

        claims.subject shouldBe "client-id"
        claims.issuer shouldBe "client-id"
        claims.audience shouldBe listOf("https://issuer.example")
        claims.getStringClaim("scope") shouldBe "scope1 scope2"
        claims.getStringClaim("resource") shouldBe "https://platform.tt02.altinn.no"
        claims.getStringClaim("jti") shouldNotBe null

        val levetidMs = claims.expirationTime.time - claims.issueTime.time
        levetidMs shouldBe 30_000L
    }

    @Test
    fun `build - signaturen kan verifiseres med tilhørende public key`() {
        val jwt = builder.build()

        jwt.verify(RSASSAVerifier(rsaKey.toRSAPublicKey())) shouldBe true
    }

    @Test
    fun `build - kalles flere ganger - gir unik jti hver gang`() {
        val jti1 = builder.build().jwtClaimsSet.getStringClaim("jti")
        val jti2 = builder.build().jwtClaimsSet.getStringClaim("jti")

        jti1 shouldNotBe jti2
    }
}

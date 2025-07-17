package no.nav.amt_altinn_acl.client.maskinporten

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.JWTBearerGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Instant
import java.util.Date
import java.util.UUID

class MaskinportenClientImpl(
	private val clientId: String,
	private val issuer: String,
	private val altinn3Url: String,
	private val scopes: List<String>,
	tokenEndpointUrl: String,
	privateJwk: String,
) : MaskinportenClient {

	private val log = LoggerFactory.getLogger(javaClass)

	private val tokenEndpoint: URI

	private val privateJwkKeyId: String

	private val assertionSigner: JWSSigner

	init {
		val rsaKey = RSAKey.parse(privateJwk)

		tokenEndpoint = URI.create(tokenEndpointUrl)
		privateJwkKeyId = rsaKey.keyID
		assertionSigner = RSASSASigner(rsaKey)
	}

	override fun hentAltinn3Token(): String {
		val signedJwt = signedClientAssertion(
			clientAssertionHeader(privateJwkKeyId),
			clientAssertionClaims(
				clientId,
				issuer,
				altinn3Url,
				scopes
			),
			assertionSigner
		)

		val request = TokenRequest(
			tokenEndpoint,
			JWTBearerGrant(signedJwt),
			Scope(*scopes.toTypedArray()),
		)

		val response = TokenResponse.parse(request.toHTTPRequest().send())

		if (!response.indicatesSuccess()) {
			val tokenErrorResponse = response.toErrorResponse()

			log.error("Failed to fetch maskinporten token. Error: {}", tokenErrorResponse.toJSONObject().toString())

			throw RuntimeException("Failed to fetch maskinporten token")
		}

		val successResponse = response.toSuccessResponse()

		val accessToken = successResponse.tokens.accessToken

		return accessToken.value
	}

	private fun signedClientAssertion(
		assertionHeader: JWSHeader,
		assertionClaims: JWTClaimsSet,
		signer: JWSSigner,
	) = SignedJWT(assertionHeader, assertionClaims).apply {
		sign(signer)
	}

	private fun clientAssertionHeader(keyId: String): JWSHeader = JWSHeader.parse(
		mapOf(
			"kid" to keyId,
			"typ" to "JWT",
			"alg" to "RS256",
		)
	)

	private fun clientAssertionClaims(
		clientId: String,
		issuer: String,
		altinnUrl: String,
		scopes: List<String>,
	): JWTClaimsSet {
		val now = Instant.now()
		val expire = now.plusSeconds(30)

		return JWTClaimsSet.Builder()
			.subject(clientId)
			.audience(issuer)
			.issuer(clientId)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(expire))
			.notBeforeTime(Date.from(now))
			.claim("scope", scopes.joinToString(" "))
			.claim("resource", altinnUrl)
			.jwtID(UUID.randomUUID().toString())
			.build()
	}
}

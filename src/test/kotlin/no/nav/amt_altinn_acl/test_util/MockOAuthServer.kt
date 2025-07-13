package no.nav.amt_altinn_acl.test_util

import no.nav.amt_altinn_acl.service.AuthService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory

class MockOAuthServer {
	private val azureAdIssuer = "azuread"
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private val server = MockOAuth2Server()
	}

	fun start() {
		try {
			server.start()
		} catch (_: IllegalArgumentException) {
			log.info("${javaClass.simpleName} is already started")
		}
	}

	fun getDiscoveryUrl(issuer: String = azureAdIssuer): String {
		return server.wellKnownUrl(issuer).toString()
	}

	fun issueAzureAdToken(
		subject: String = "test",
		audience: String = "test-aud",
		claims: Map<String, Any> = emptyMap()
	): String {
		return server.issueToken(azureAdIssuer, subject, audience, claims).serialize()
	}

	fun issueAzureAdM2MToken(
		subject: String = "test",
		audience: String = "test-aud",
		claims: Map<String, Any> = emptyMap()
	): String {
		val claimsWithRoles = claims.toMutableMap()
		claimsWithRoles["roles"] = arrayOf(AuthService.ACCESS_AS_APPLICATION_ROLE)

		return server.issueToken(azureAdIssuer, subject, audience, claimsWithRoles).serialize()
	}
}

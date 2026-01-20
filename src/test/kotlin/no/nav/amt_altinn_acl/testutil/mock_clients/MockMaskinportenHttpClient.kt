package no.nav.amt_altinn_acl.testutil.mock_clients

import no.nav.amt_altinn_acl.testutil.MockHttpClient
import no.nav.amt_altinn_acl.testutil.TokenCreator
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

class MockMaskinportenHttpClient : MockHttpClient() {
	fun enqueueTokenResponse() {
		val token = TokenCreator.instance().createToken()

		enqueue(
			headers = mapOf(HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE),
			body = """{ "token_type": "Bearer", "access_token": "$token", "expires": 3600 }""",
		)
	}
}

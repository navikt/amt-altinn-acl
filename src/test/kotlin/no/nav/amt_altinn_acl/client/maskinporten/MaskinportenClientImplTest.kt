package no.nav.amt_altinn_acl.client.maskinporten

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.test_util.Constants.TEST_JWK
import no.nav.amt_altinn_acl.test_util.TokenCreator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Arrays

class MaskinportenClientImplTest {
	private lateinit var mockServer: MockWebServer

	@BeforeEach
	fun start() {
		mockServer = MockWebServer()
		mockServer.start()
	}

	@AfterEach
	fun shutdown() = mockServer.shutdown()

	private fun mockServerUrl(): String = mockServer.url("").toString().removeSuffix("/")

	@Test
	fun `skal lage riktig request og parse response`() {
		val accessToken: String = TokenCreator.instance().createToken()

		mockServer.enqueue(tokenMockResponse(accessToken))

		val scope1 = "scope1"
		val scope2 = "scope2"

		val client = MaskinportenClientImpl(
			clientId = "client-id",
			issuer = "issuer",
			altinn3Url = "https://platform.tt02.altinn.no",
			scopes = listOf(scope1, scope2),
			tokenEndpointUrl = mockServerUrl() + "/token",
			privateJwk = TEST_JWK
		)

		val token = client.hentAltinn3Token()
		val recordedRequest = mockServer.takeRequest()
		val data: Map<String, String> = parseFormData(recordedRequest.body.readUtf8())

		token shouldBe accessToken
		recordedRequest.path shouldBe "/token"
		recordedRequest.method shouldBe "POST"
		data["grant_type"] shouldBe "urn:ietf:params:oauth:grant-type:jwt-bearer"
		data["scope"] shouldBe "$scope1 $scope2"
		data["assertion"] shouldNotBe null
	}

	private fun parseFormData(formData: String): Map<String, String> {
		val data: MutableMap<String, String> = HashMap()
		val values = formData.split("&").toTypedArray()

		Arrays.stream(values).forEach { v: String ->
			val keyValue = v.split("=").toTypedArray()
			data[keyValue[0]] = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
		}

		return data
	}

	private fun tokenMockResponse(accessToken: String): MockResponse {
		val body = """
			{ "token_type": "Bearer", "access_token": "$accessToken", "expires": 3600 }
		""".trimIndent()

		return MockResponse()
			.setBody(body)
			.setHeader("Content-Type", "application/json")
	}
}

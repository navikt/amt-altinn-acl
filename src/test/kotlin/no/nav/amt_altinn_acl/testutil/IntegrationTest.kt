package no.nav.amt_altinn_acl.testutil

import no.nav.amt_altinn_acl.testutil.Constants.TEST_JWK
import no.nav.amt_altinn_acl.testutil.mock_clients.MockAltinnHttpServer
import no.nav.amt_altinn_acl.testutil.mock_clients.MockMaskinportenHttpClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration

@Import(TestConfig::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest : RepositoryTestBase() {
	@LocalServerPort
	private var port: Int = 0

	private val client =
		OkHttpClient
			.Builder()
			.callTimeout(Duration.ofMinutes(5))
			.build()

	companion object {
		val oAuthServer = MockOAuthServer()
		val mockAltinnHttpClient = MockAltinnHttpServer()
		val mockMaskinportenHttpClient = MockMaskinportenHttpClient()

		@JvmStatic
		@DynamicPropertySource
		@Suppress("unused")
		fun registerProperties(registry: DynamicPropertyRegistry) {
			oAuthServer.start()
			mockAltinnHttpClient.start()
			mockMaskinportenHttpClient.start()

			registry.add("no.nav.security.jwt.issuer.azuread.discovery-url", oAuthServer::getDiscoveryUrl)
			registry.add("no.nav.security.jwt.issuer.azuread.accepted-audience") { "test-aud" }

			registry.add("altinn3.url", mockAltinnHttpClient::serverUrl)

			registry.add("maskinporten.scopes") { "scope1 scope2" }
			registry.add("maskinporten.client-id") { "abc123" }
			registry.add("maskinporten.issuer") { "https://test-issuer" }
			registry.add("maskinporten.token-endpoint") { mockMaskinportenHttpClient.serverUrl() }
			registry.add("maskinporten.client-jwk") { TEST_JWK }
		}
	}

	fun serverUrl() = "http://localhost:$port"

	protected fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap(),
	): Response {
		val reqBuilder =
			Request
				.Builder()
				.url("${serverUrl()}$path")
				.method(method, body)

		headers.forEach {
			reqBuilder.addHeader(it.key, it.value)
		}

		return client.newCall(reqBuilder.build()).execute()
	}
}

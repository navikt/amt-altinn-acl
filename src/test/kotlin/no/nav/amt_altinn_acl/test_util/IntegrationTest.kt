package no.nav.amt_altinn_acl.test_util

import no.nav.amt_altinn_acl.test_util.Constants.TEST_JWK
import no.nav.amt_altinn_acl.test_util.mock_clients.MockAltinnHttpClient
import no.nav.amt_altinn_acl.test_util.mock_clients.MockMaskinportenHttpClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration

@ActiveProfiles("test")
@Import(TestConfig::class)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {

	@LocalServerPort
	private var port: Int = 0

	private val client = OkHttpClient.Builder()
		.callTimeout(Duration.ofMinutes(5))
		.build()

	@AfterEach
	fun cleanDatabase() {
		DbTestDataUtils.cleanDatabase(postgresDataSource)
		mockAltinnHttpClient.resetRequestCount()
		mockMaskinportenHttpClient.resetRequestCount()
	}

	companion object {
		val oAuthServer = MockOAuthServer()
		val mockAltinnHttpClient = MockAltinnHttpClient()
		val mockMaskinportenHttpClient = MockMaskinportenHttpClient()
		val postgresDataSource = SingletonPostgresContainer.getDataSource()

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			oAuthServer.start()
			mockAltinnHttpClient.start()
			mockMaskinportenHttpClient.start()

			registry.add("no.nav.security.jwt.issuer.azuread.discovery-url", oAuthServer::getDiscoveryUrl)
			registry.add("no.nav.security.jwt.issuer.azuread.accepted-audience") { "test-aud" }

			registry.add("altinn.koordinator-service-code") { "99999" }
			registry.add("altinn.veileder-service-code") { "88888" }
			registry.add("altinn.url", mockAltinnHttpClient::serverUrl)
			registry.add("altinn.api-key") { "test-altinn-api-key" }

			registry.add("maskinporten.scopes") { "scope1 scope2" }
			registry.add("maskinporten.client-id") { "abc123" }
			registry.add("maskinporten.issuer") { "https://test-issuer" }
			registry.add("maskinporten.token-endpoint") { mockMaskinportenHttpClient.serverUrl() }
			registry.add("maskinporten.client-jwk") { TEST_JWK }

			val container = SingletonPostgresContainer.getContainer()

			registry.add("spring.datasource.url") { container.jdbcUrl }
			registry.add("spring.datasource.username") { container.username }
			registry.add("spring.datasource.password") { container.password }
			registry.add("spring.datasource.hikari.maximum-pool-size") { 3 }
		}
	}

	fun serverUrl() = "http://localhost:$port"

	fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap()
	): Response {
		val reqBuilder = Request.Builder()
			.url("${serverUrl()}$path")
			.method(method, body)

		headers.forEach {
			reqBuilder.addHeader(it.key, it.value)
		}

		return client.newCall(reqBuilder.build()).execute()
	}

}

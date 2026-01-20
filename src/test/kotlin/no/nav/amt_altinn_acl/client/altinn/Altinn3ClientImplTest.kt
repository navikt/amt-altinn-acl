package no.nav.amt_altinn_acl.client.altinn

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt_altinn_acl.domain.RolleType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import tools.jackson.module.kotlin.jacksonObjectMapper

class Altinn3ClientImplTest {
	private val mockServer: MockWebServer = MockWebServer()

	private fun mockServerUrl(): String = mockServer.url("").toString().removeSuffix("/")

	private val altinnClient =
		Altinn3ClientImpl(
			baseUrl = mockServerUrl(),
			maskinportenTokenProvider = { "TOKEN" },
			objectMapper = jacksonObjectMapper(),
		)

	@Test
	fun `hentRoller - flere tilganger - parser response riktig`() {
		val resourceIds = listOf(RolleType.KOORDINATOR.resourceId, RolleType.VEILEDER.resourceId, "resource3")
		val jsonResponse =
			"""
			[
			  {
			    "organizationNumber": "123456789",
			    "authorizedResources": ["${resourceIds[0]}", "${resourceIds[1]}"],
			    "subunits": []
			  },
			  {
			    "organizationNumber": "987654321",
			    "authorizedResources": ["${resourceIds[2]}"],
			    "subunits": [
			      {
			        "organizationNumber": "111222333",
			        "authorizedResources": ["${resourceIds[1]}"],
			        "subunits": []
			      }
			    ]
			  },
			  {
			    "organizationNumber": "456789012",
			    "authorizedResources": ["${resourceIds[0]}", "${resourceIds[2]}"],
			    "subunits": [
			      {
			        "organizationNumber": "333444555",
			        "authorizedResources": ["${resourceIds[1]}"],
			        "subunits": [
			          {
			            "organizationNumber": "666777888",
			            "authorizedResources": ["${resourceIds[0]}"],
			            "subunits": []
			          }
			        ]
			      }
			    ]
			  }
			]
			""".trimIndent()

		mockServer.enqueue(
			MockResponse()
				.setBody(jsonResponse)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
		)

		val norskIdent = "123456"

		val organisasjoner = altinnClient.hentRoller(norskIdent, RolleType.entries)

		val request = mockServer.takeRequest()

		request.method shouldBe "POST"

		val koordinatorRoller = organisasjoner[RolleType.KOORDINATOR]
		koordinatorRoller.shouldNotBeNull()
		koordinatorRoller shouldHaveSize 3

		val veilederRoller = organisasjoner[RolleType.VEILEDER]
		veilederRoller.shouldNotBeNull()
		veilederRoller shouldHaveSize 3
	}
}

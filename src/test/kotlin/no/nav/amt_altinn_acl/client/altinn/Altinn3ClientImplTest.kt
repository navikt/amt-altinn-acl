package no.nav.amt_altinn_acl.client.altinn

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Altinn3ClientImplTest {

	private val mockServer: MockWebServer = MockWebServer()

	private fun mockServerUrl(): String {
		return mockServer.url("").toString().removeSuffix("/")
	}

	private val altinnClient = Altinn3ClientImpl(
		baseUrl = mockServerUrl(),
		//altinnApiKey = "api-key",
		maskinportenTokenProvider = { "TOKEN" }
	)

	@Test
	fun `hentAlleOrganisasjoner - flere tilganger - parser response riktig`() {
		val resourceIds = listOf(RolleType.KOORDINATOR.resourceId, RolleType.VEILEDER.resourceId, "resource3")
		val jsonResponse = """
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
				.setHeader("Content-Type", "application/json")
		)

		val norskIdent = "123456"

		val organisasjoner = altinnClient.hentAlleOrganisasjoner(norskIdent, RolleType.entries)

		val request = mockServer.takeRequest()

		request.method shouldBe "POST"

		organisasjoner[RolleType.KOORDINATOR]!! shouldHaveSize 3
		organisasjoner[RolleType.VEILEDER]!! shouldHaveSize 3
	}
}

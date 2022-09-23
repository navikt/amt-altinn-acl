package no.nav.amt_altinn_acl.client.altinn

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AltinnClientImplTest {

	private lateinit var mockServer: MockWebServer

	@BeforeEach
	fun start() {
		mockServer = MockWebServer()
		mockServer.start()
	}

	@AfterEach
	fun shutdown() {
		mockServer.shutdown()
	}

	private fun mockServerUrl(): String {
		return mockServer.url("").toString().removeSuffix("/")
	}

	@Test
	fun `hentTilknyttedeOrganisasjoner - skal lage riktig request og parse response`() {
		val altinnClient = AltinnClientImpl(
			baseUrl = mockServerUrl(),
			altinnApiKey = "api-key",
			maskinportenTokenProvider = {"TOKEN"}
		)

		val jsonResponse = """
			[
				{
					"Name": "LAGSPORT PLUTSELIG ",
					"Type": "Person",
					"SocialSecurityNumber": "11111111111"
				},
				{
					"Name": "NONFIGURATIV KOMFORTABEL HUND DA",
					"Type": "Enterprise",
					"OrganizationNumber": "999987004",
					"OrganizationForm": "DA",
					"Status": "Active"
				},
				{
					"Name": "NONFIGURATIV KOMFORTABEL HUND DA",
					"Type": "Business",
					"OrganizationNumber": "999919596",
					"ParentOrganizationNumber": "999987004",
					"OrganizationForm": "BEDR",
					"Status": "Active"
				},
				{
					"Name": "NØDVENDIG NESTE KATT INDUSTRI",
					"Type": "Enterprise",
					"OrganizationNumber": "999906097",
					"OrganizationForm": "ENK",
					"Status": "Active"
				},
				{
					"Name": "NØDVENDIG NESTE KATT INDUSTRI",
					"Type": "Business",
					"OrganizationNumber": "999928026",
					"ParentOrganizationNumber": "999906097",
					"OrganizationForm": "BEDR",
					"Status": "Active"
				}
			]
		""".trimIndent()

		mockServer.enqueue(
			MockResponse()
				.setBody(jsonResponse)
				.setHeader("Content-Type", "application/json")
		)

		val norskIdent = "123456"

		val organisasjoner = altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)

		val request = mockServer.takeRequest()

		request.method shouldBe "GET"
		request.path shouldBe "/api/serviceowner/reportees?subject=$norskIdent"
		request.headers["APIKEY"] shouldBe "api-key"
		request.headers["Authorization"] shouldBe "Bearer TOKEN"

		organisasjoner shouldHaveSize 4
		organisasjoner.filter { it.type == Organisasjon.Type.OVERORDNET_ENHET } shouldHaveSize 2
		organisasjoner.filter { it.type == Organisasjon.Type.UNDERENHET } shouldHaveSize 2
	}

	@Test
	fun `hentRettigheter - skal lage riktig request og parse response`() {
		val altinnClient = AltinnClientImpl(
			baseUrl = mockServerUrl(),
			altinnApiKey = "api-key",
			maskinportenTokenProvider = {"TOKEN"}
		)

		val jsonResponse = """
			{
				"Subject": {
					"Name": "LAGSPORT PLUTSELIG ",
					"Type": "Person",
					"SocialSecurityNumber": "99999098174"
				},
				"Reportee": {
					"Name": "NONFIGURATIV KOMFORTABEL HUND DA",
					"Type": "Business",
					"OrganizationNumber": "999919596",
					"OrganizationForm": "BEDR",
					"Status": "Active"
				},
				"Rights": [
					{
						"ServiceCode": "3234",
						"Action": "Read",
						"RightID": 123456,
						"RightType": "Service",
						"ServiceEditionCode": 1,
						"RightSourceType": "RoleTypeRights",
						"IsDelegatable": true
					},
					{
						"ServiceCode": "1234",
						"Action": "Write",
						"RightID": 9061224,
						"RightType": "Service",
						"ServiceEditionCode": 1,
						"RightSourceType": "RoleTypeRights",
						"IsDelegatable": true
					},
					{
						"ServiceCode": "5678",
						"Action": "Sign",
						"RightID": 9062625,
						"RightType": "Service",
						"ServiceEditionCode": 1,
						"RightSourceType": "RoleTypeRights",
						"IsDelegatable": true
					}
				]
			}
		""".trimIndent()

		mockServer.enqueue(
			MockResponse()
				.setBody(jsonResponse)
				.setHeader("Content-Type", "application/json")
		)

		val norskIdent = "123456"
		val organisasjonsnummer = "34823784"

		val rettigheter = altinnClient.hentRettigheter(norskIdent, organisasjonsnummer)

		val request = mockServer.takeRequest()

		request.method shouldBe "GET"
		request.path shouldBe "/api/serviceowner/authorization/rights?subject=$norskIdent&reportee=$organisasjonsnummer"
		request.headers["APIKEY"] shouldBe "api-key"
		request.headers["Authorization"] shouldBe "Bearer TOKEN"

		rettigheter shouldHaveSize 3

		rettigheter.any { it.serviceCode == "3234" } shouldBe true
		rettigheter.any { it.serviceCode == "5678" } shouldBe true
		rettigheter.any { it.serviceCode == "1234" } shouldBe true
	}

}

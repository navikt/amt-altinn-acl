package no.nav.amt_altinn_acl.client.altinn

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.amt_altinn_acl.utils.JsonUtils
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
	fun `hentAlleOrganisasjoner - 4 tilganger - kun et kall til Altinn`() {
		val serviceCode = "5858"
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

		val organisasjoner = altinnClient.hentAlleOrganisasjoner(norskIdent, serviceCode)

		val request = mockServer.takeRequest()

		request.method shouldBe "GET"
		request.path shouldBe "/api/serviceowner/reportees?subject=$norskIdent&serviceCode=$serviceCode&serviceEdition=1&\$top=$pagineringSize&\$skip=0"
		request.headers["APIKEY"] shouldBe "api-key"
		request.headers["Authorization"] shouldBe "Bearer TOKEN"


		organisasjoner shouldHaveSize 4
	}

	@Test
	fun `hentAlleOrganisasjoner - 505 tilganger - to kall til Altinn`() {
		val serviceCode = "5858"
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
				.setBody(JsonUtils.objectMapper.writeValueAsString(getAltinnResponse()))
				.setHeader("Content-Type", "application/json")
		)
		mockServer.enqueue(
			MockResponse()
				.setBody(jsonResponse)
				.setHeader("Content-Type", "application/json")
		)

		val norskIdent = "123456"

		val organisasjoner = altinnClient.hentAlleOrganisasjoner(norskIdent, serviceCode)

		mockServer.requestCount shouldBe 2
		organisasjoner shouldHaveSize 505
	}
}

private fun getAltinnResponse(size: Int = 501): List<AltinnClientImpl.ReporteeResponseEntity.Reportee> {
	val response = mutableListOf<AltinnClientImpl.ReporteeResponseEntity.Reportee>()
	var i = 0
	while (response.size < size) {
		response.add(
			AltinnClientImpl.ReporteeResponseEntity.Reportee(
				type = "Type + $i",
				organisasjonsnummer = i.toString()
			)
		)
		i++
	}
	return response
}

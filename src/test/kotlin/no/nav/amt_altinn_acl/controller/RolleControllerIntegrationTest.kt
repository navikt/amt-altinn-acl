package no.nav.amt_altinn_acl.controller

import io.kotest.matchers.shouldBe
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*

class RolleControllerIntegrationTest : IntegrationTest() {

	@AfterEach
	internal fun tearDown() {
		mockAltinnHttpClient.resetHttpServer()
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return 401 when not authenticated`() {
		val response = sendRequest(
			method = "GET",
			path = "/api/v1/rolle/tiltaksarrangor?norskIdent=4273684",
		)

		response.code shouldBe 401
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return 403 when not machine-to-machine request`() {
		val response = sendRequest(
			method = "GET",
			path = "/api/v1/rolle/tiltaksarrangor?norskIdent=4273684",
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdToken()}")
		)

		response.code shouldBe 403
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return 200 with correct response`() {
		val norskIdent = UUID.randomUUID().toString()
		val orgnr = "1234567"

		mockMaskinportenHttpClient.enqueueTokenResponse()

		mockAltinnHttpClient.addReporteeResponse(norskIdent, altinnKoordinatorServiceKode, listOf(orgnr))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, altinnVeilederServiceKode, listOf(orgnr))


		val response = sendRequest(
			method = "GET",
			path = "/api/v1/rolle/tiltaksarrangor?norskIdent=$norskIdent",
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val expectedJson = """
			{"roller":[{"organisasjonsnummer":"$orgnr","roller":["VEILEDER","KOORDINATOR"]}]}
		""".trimIndent()

		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return cached response from altinn`() {
		val personIdent = UUID.randomUUID().toString()
		val orgnr = "1234567"

		mockMaskinportenHttpClient.enqueueTokenResponse()

		mockAltinnHttpClient.addReporteeResponse(personIdent, altinnKoordinatorServiceKode, listOf(orgnr))
		mockAltinnHttpClient.addReporteeResponse(personIdent, altinnVeilederServiceKode, emptyList())

		val response1 = sendRequest(
			method = "GET",
			path = "/api/v1/rolle/tiltaksarrangor?norskIdent=$personIdent",
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val response2 = sendRequest(
			method = "GET",
			path = "/api/v1/rolle/tiltaksarrangor?norskIdent=$personIdent",
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val expectedJson = """
			{"roller":[{"organisasjonsnummer":"$orgnr","roller":["KOORDINATOR"]}]}
		""".trimIndent()

		response1.code shouldBe 200
		response1.body?.string() shouldBe expectedJson

		response2.code shouldBe 200
		response2.body?.string() shouldBe expectedJson

		mockAltinnHttpClient.requestCount() shouldBe 2
	}

}

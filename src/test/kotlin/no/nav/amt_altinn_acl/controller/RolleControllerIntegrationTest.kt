package no.nav.amt_altinn_acl.controller

import io.kotest.matchers.shouldBe
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class RolleControllerIntegrationTest : IntegrationTest() {
	private val mediaTypeJson = "application/json".toMediaType()

	@AfterEach
	internal fun tearDown() {
		mockAltinnHttpClient.resetHttpServer()
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return 401 when not authenticated`() {
		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rolle/tiltaksarrangor",
			body = """{"personident": "12345678910"}""".toRequestBody(mediaTypeJson)
		)

		response.code shouldBe 401
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return 403 when not machine-to-machine request`() {
		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rolle/tiltaksarrangor",
			body = """{"personident": "12345678910"}""".toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdToken()}")
		)

		response.code shouldBe 403
	}

	@Test
	fun `hentTiltaksarrangorRoller - returnerer 400 hvis personident har feil format`() {
		val norskIdent = "1234567891K"

		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rolle/tiltaksarrangor",
			body = """{"personident": "$norskIdent"}""".toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		response.code shouldBe 400
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return 200 with correct response`() {
		val norskIdent = "12345678910"
		val orgnr = "1234567"

		mockMaskinportenHttpClient.enqueueTokenResponse()

		mockAltinnHttpClient.addReporteeResponse(norskIdent, RolleType.KOORDINATOR.serviceCode, listOf(orgnr))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, RolleType.VEILEDER.serviceCode, listOf(orgnr))


		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rolle/tiltaksarrangor",
			body = """{"personident": "$norskIdent"}""".toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val expectedJson = """
			{"roller":[{"organisasjonsnummer":"$orgnr","roller":["KOORDINATOR","VEILEDER"]}]}
		""".trimIndent()

		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `hentTiltaksarrangorRoller - should return cached response from altinn`() {
		val personIdent = "12345678910"
		val orgnr = "1234567"

		mockMaskinportenHttpClient.enqueueTokenResponse()

		mockAltinnHttpClient.addReporteeResponse(personIdent, RolleType.KOORDINATOR.serviceCode, listOf(orgnr))
		mockAltinnHttpClient.addReporteeResponse(personIdent, RolleType.VEILEDER.serviceCode, emptyList())

		val response1 = sendRequest(
			method = "POST",
			path = "/api/v1/rolle/tiltaksarrangor",
			body = """{"personident": "$personIdent"}""".toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val response2 = sendRequest(
			method = "POST",
			path = "/api/v1/rolle/tiltaksarrangor",
			body = """{"personident": "$personIdent"}""".toRequestBody(mediaTypeJson),
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

package no.nav.amt_altinn_acl.controller

import io.kotest.matchers.shouldBe
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import no.nav.amt_altinn_acl.utils.RestUtils.toJsonRequestBody
import org.junit.jupiter.api.Test

class RettigheterControllerIntegrationTest : IntegrationTest() {

	@Test
	fun `hentRettigheter - should return 401 when not authenticated`() {
		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rettighet/hent",
			body = """{"norskIdent": "4273684", "rettighetIder": []}""".toJsonRequestBody()
		)

		response.code shouldBe 401
	}

	@Test
	fun `hentRettigheter - should return 403 when not machine-to-machine request`() {
		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rettighet/hent",
			body = """{"norskIdent": "4273684", "rettighetIder": []}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdToken()}")
		)

		response.code shouldBe 403
	}

	@Test
	fun `hentRettigheter - should return 200 with correct response`() {
		val orgnr = "1234567"
		val rettighetId1 = 1234L
		val rettighetId2 = 5678L
		val rettighetId3 = 34872L

		mockMaskinportenHttpClient.enqueueTokenResponse()

		mockAltinnHttpClient.enqueueHentTilknyttedeEnheterResponse(listOf(orgnr))

		mockAltinnHttpClient.enqueueHentRettigheterResponse(listOf(rettighetId1, rettighetId2, rettighetId3))

		val response = sendRequest(
			method = "POST",
			path = "/api/v1/rettighet/hent",
			body = """{"norskIdent": "4273684", "rettighetIder": ["1234", "5678"]}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val expectedJson = """
			{"rettigheter":[{"id":"1234","organisasjonsnummer":"1234567"},{"id":"5678","organisasjonsnummer":"1234567"}]}
		""".trimIndent()

		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `hentRettigheter - should return cache response from altinn`() {
		val orgnr = "1234567"
		val rettighetId1 = 1234L
		val rettighetId2 = 5678L
		val rettighetId3 = 34872L

		mockMaskinportenHttpClient.enqueueTokenResponse()

		mockAltinnHttpClient.enqueueHentTilknyttedeEnheterResponse(listOf(orgnr))

		mockAltinnHttpClient.enqueueHentRettigheterResponse(listOf(rettighetId1, rettighetId2, rettighetId3))

		val response1 = sendRequest(
			method = "POST",
			path = "/api/v1/rettighet/hent",
			body = """{"norskIdent": "4273684", "rettighetIder": ["1234", "5678"]}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val response2 = sendRequest(
			method = "POST",
			path = "/api/v1/rettighet/hent",
			body = """{"norskIdent": "4273684", "rettighetIder": ["1234", "5678"]}""".toJsonRequestBody(),
			headers = mapOf("Authorization" to "Bearer ${oAuthServer.issueAzureAdM2MToken()}")
		)

		val expectedJson = """
			{"rettigheter":[{"id":"1234","organisasjonsnummer":"1234567"},{"id":"5678","organisasjonsnummer":"1234567"}]}
		""".trimIndent()

		response1.code shouldBe 200
		response1.body?.string() shouldBe expectedJson

		response2.code shouldBe 200
		response2.body?.string() shouldBe expectedJson

		mockAltinnHttpClient.requestCount() shouldBe 2
	}

}

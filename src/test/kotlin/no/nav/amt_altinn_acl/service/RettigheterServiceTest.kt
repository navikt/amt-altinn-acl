package no.nav.amt_altinn_acl.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.repository.RettigheterCacheRepository
import no.nav.amt_altinn_acl.repository.dbo.RettigheterCacheDbo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class RettigheterServiceTest {

	lateinit var altinnClient: AltinnClient

	lateinit var rettigheterCacheRepository: RettigheterCacheRepository

	lateinit var rettigheterService: RettigheterService

	@BeforeEach
	fun setup() {
		altinnClient = mockk()
		rettigheterCacheRepository = mockk()
		rettigheterService = RettigheterService(altinnClient, rettigheterCacheRepository)
	}

	@Test
	fun `hentAlleRettigheter - skal hente rettigheter fra cache`() {
		val norskIdent = "21313"
		val organisasjonsnummer = "34532534"
		val serviceCode = "432438"

		every {
			rettigheterCacheRepository.hentCachetData(norskIdent, 2)
		} returns RettigheterCacheDbo(
			1, norskIdent, 1, """
			{
			  "rettigheter": [
				{
				  "serviceCode": "$serviceCode",
				  "organisasjonsnummer": "$organisasjonsnummer"
				}
			  ]
       		 }
		""".trimIndent(), ZonedDateTime.now().plusHours(1), ZonedDateTime.now()
		)

		val rettigheter = rettigheterService.hentAlleRettigheter(norskIdent)

		rettigheter shouldHaveSize 1
		rettigheter.first().serviceCode shouldBe serviceCode
		rettigheter.first().organisasjonsnummer shouldBe organisasjonsnummer

		verify(exactly = 0) {
			altinnClient.hentRettigheter(any(), any())
		}
	}

}

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
		val rettighetId = "432438"

		every {
			rettigheterCacheRepository.hentRettigheter(norskIdent)
		} returns RettigheterCacheDbo(
			1, norskIdent, """
			{
			  "version": 1,
			  "rettigheter": [
				{
				  "rettighetId": "$rettighetId",
				  "organisasjonsnummmer": "$organisasjonsnummer"
				}
			  ]
       		 }
		""".trimIndent(), ZonedDateTime.now().plusHours(1), ZonedDateTime.now()
		)

		val rettigheter = rettigheterService.hentAlleRettigheter(norskIdent)

		rettigheter shouldHaveSize 1
		rettigheter.first().rettighetId shouldBe rettighetId
		rettigheter.first().organisasjonsnummmer shouldBe organisasjonsnummer

		verify(exactly = 0) {
			altinnClient.hentRettigheter(any(), any())
		}
	}

}

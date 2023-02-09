package no.nav.amt_altinn_acl.service

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.client.altinn.AltinnRettighet
import no.nav.amt_altinn_acl.client.altinn.Organisasjon
import no.nav.amt_altinn_acl.repository.RettigheterCacheRepository
import no.nav.amt_altinn_acl.repository.dbo.RettigheterCacheDbo
import no.nav.amt_altinn_acl.service.RettigheterService.Companion.CACHE_VERSION
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class RettigheterServiceTest {

	val koordinatorServiceKode = "1234"

	val veilederServiceKode = "5678"

	lateinit var altinnClient: AltinnClient

	lateinit var rettigheterCacheRepository: RettigheterCacheRepository

	lateinit var rettigheterService: RettigheterService

	@BeforeEach
	fun setup() {
		altinnClient = mockk()
		rettigheterCacheRepository = mockk()
		rettigheterService = RettigheterService(
			altinnKoordinatorServiceCode = koordinatorServiceKode,
			altinnVeilederServiceCode = veilederServiceKode,
			altinnClient,
			rettigheterCacheRepository
		)
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

	@Test
	fun `hentAlleRettigheter - skal bruke expired data hvis kall til altinn feiler`() {
		val norskIdent = "21313"
		val organisasjonsnummer = "34532534"
		val serviceCode = "432438"

		every {
			rettigheterCacheRepository.hentCachetData(norskIdent, 2)
		} returns RettigheterCacheDbo(
			1, norskIdent, 2, """
			{
			  "rettigheter": [
				{
				  "serviceCode": "$serviceCode",
				  "organisasjonsnummer": "$organisasjonsnummer"
				}
			  ]
       		 }
		""".trimIndent(), ZonedDateTime.now().minusHours(1), ZonedDateTime.now()
		)

		every {
			altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)
		} throws RuntimeException()

		val rettigheter = rettigheterService.hentAlleRettigheter(norskIdent)

		rettigheter shouldHaveSize 1
		rettigheter.first().serviceCode shouldBe serviceCode
		rettigheter.first().organisasjonsnummer shouldBe organisasjonsnummer
	}

	@Test
	fun `hentAlleRettigheter - skal feile hvis ingen cachet data og kall til Altinn feiler`() {
		val norskIdent = "21313"

		every {
			rettigheterCacheRepository.hentCachetData(norskIdent, 2)
		} returns null

		every {
			altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)
		} throws RuntimeException()

		shouldThrowExactly<RuntimeException> { rettigheterService.hentAlleRettigheter(norskIdent) }
	}


	@Test
	fun `hentAlleRettigheter - bruker finnes ikke i cache - skal cache`() {
		val norskIdent = "21313"
		val organisasjonsnummer = "34532534"
		val serviceCode = "432438"

		every {
			altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)
		} returns listOf(Organisasjon(organisasjonsnummer, Organisasjon.Type.UNDERENHET))

		every {
			altinnClient.hentRettigheter(norskIdent, organisasjonsnummer)
		} returns listOf(
			AltinnRettighet(serviceCode),
			AltinnRettighet(koordinatorServiceKode)
		)

		every {
			rettigheterCacheRepository.hentCachetData(norskIdent, 2)
		} returns null

		every {
			rettigheterCacheRepository.upsertData(norskIdent, 2, any(), any())
		} returns Unit

		rettigheterService.hentAlleRettigheter(norskIdent)

		val expectedJson = """
			{"rettigheter":[{"organisasjonsnummer":"34532534","serviceCode":"1234"}]}
		""".trimIndent()

		verify(exactly = 1) {
			rettigheterCacheRepository.upsertData(norskIdent, 2, expectedJson, any())
		}
	}

	@Test
	fun `hentAlleRettigheter - bruker har rettigheter i cache men ikke i altinn - skal caches`() {
		val norskIdent = "21313"
		val organisasjonsnummer = "34532534"
		val serviceCode = "343092"
		val tommeRettigheter = """{"rettigheter":[]}"""
		every {
			altinnClient.hentTilknyttedeOrganisasjoner(norskIdent)
		} returns listOf(Organisasjon(organisasjonsnummer, Organisasjon.Type.UNDERENHET))

		every {
			rettigheterCacheRepository.hentCachetData(norskIdent, CACHE_VERSION)
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
		""".trimIndent(), ZonedDateTime.now().minusHours(1), ZonedDateTime.now()
		)

		every {
			altinnClient.hentRettigheter(norskIdent, organisasjonsnummer)
		} returns emptyList()

		every {
			rettigheterCacheRepository.upsertData(norskIdent, CACHE_VERSION, any(), any())
		} returns Unit

		val rettigheter = rettigheterService.hentAlleRettigheter(norskIdent)

		rettigheter shouldHaveSize 0

		verify(exactly = 1) {
			rettigheterCacheRepository.upsertData(norskIdent, CACHE_VERSION, tommeRettigheter, any())
		}
	}

}

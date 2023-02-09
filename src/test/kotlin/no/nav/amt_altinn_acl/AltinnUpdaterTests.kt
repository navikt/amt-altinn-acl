package no.nav.amt_altinn_acl

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.jobs.AltinnUpdater
import no.nav.amt_altinn_acl.repository.RettigheterCacheRepository
import no.nav.amt_altinn_acl.service.RettigheterService
import no.nav.amt_altinn_acl.service.RettigheterService.Companion.CACHE_VERSION
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import no.nav.amt_altinn_acl.utils.JsonUtils
import no.nav.amt_altinn_acl.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZonedDateTime
import kotlin.random.Random

class AltinnUpdaterTests {
	private lateinit var rettigheterService: RettigheterService
	private lateinit var altinnUpdater: AltinnUpdater
	private lateinit var altinnClient: AltinnClient
	private lateinit var rettigheterCacheRepository: RettigheterCacheRepository
	private val dataSource = SingletonPostgresContainer.getDataSource()

	private val koordinatorServiceKode = "123"
	private val veilederServiceKode = "456"

	@BeforeEach
	fun setup() {
		altinnClient = mockk()
		rettigheterCacheRepository = RettigheterCacheRepository(
			NamedParameterJdbcTemplate(dataSource)
		)
		rettigheterService = RettigheterService(
			koordinatorServiceKode,
			veilederServiceKode,
			altinnClient,
			rettigheterCacheRepository
		)
		altinnUpdater = AltinnUpdater(
			rettigheterService
		)
	}

	@Test
	fun `update - utdatert bruker - skal synkronisere bruker`() {
		val organisasjonsnummer = "2131"
		val emptyRettigheterData = RettigheterService.CachetRettigheter(emptyList())
		val personligIdent = Random.nextLong().toString()

		rettigheterCacheRepository.upsertData(norskIdent = personligIdent, CACHE_VERSION, toJsonString(emptyRettigheterData), ZonedDateTime.now().minusMinutes(1))

		every {
			altinnClient.hentOrganisasjoner(personligIdent, koordinatorServiceKode)
		} returns Result.success(listOf(organisasjonsnummer))


		every {
			altinnClient.hentOrganisasjoner(personligIdent, veilederServiceKode)
		} returns Result.success(emptyList())

		altinnUpdater.update()

		val oppdatertBruker = rettigheterCacheRepository.hentCachetData(personligIdent, CACHE_VERSION)
		val nyeRettigheter = oppdatertBruker?.let { JsonUtils.fromJsonString<RettigheterService.CachetRettigheter>(it.dataJson) }

		nyeRettigheter!!.rettigheter.size shouldBe 1
		nyeRettigheter.rettigheter shouldBe listOf(AltinnRettighet(
			organisasjonsnummer = organisasjonsnummer,
			serviceCode = koordinatorServiceKode
		))

	}


}

package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.service.RettigheterService
import no.nav.amt_altinn_acl.test_util.DbTestDataUtils
import no.nav.amt_altinn_acl.test_util.DbTestDataUtils.shouldBeEqualTo
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import no.nav.amt_altinn_acl.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZonedDateTime

class RettigheterCacheRepositoryTest {

	private val dataSource = SingletonPostgresContainer.getDataSource()

	private val repository = RettigheterCacheRepository(NamedParameterJdbcTemplate(dataSource))

	@AfterEach
	fun cleanup() {
		DbTestDataUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `skal upserte og hente rettigheter`() {
		val norskIdent = "1243234"

		val data = RettigheterService.CachetRettigheter(
			listOf(AltinnRettighet("123", "4367842"))
		)

		val expiration = ZonedDateTime.now().plusHours(12)

		repository.upsertData(norskIdent, 1, toJsonString(data), expiration)

		val cachetRettighet = repository.hentCachetData(norskIdent, 1)

		val expectedJson = """
			{"rettigheter":[{"organisasjonsnummer":"123","serviceCode":"4367842"}]}
		""".trimIndent()

		cachetRettighet shouldNotBe null
		cachetRettighet?.dataVersion shouldBe 1
		cachetRettighet?.dataJson shouldBe expectedJson
	}

	@Test
	fun `upsert skal oppdatere rettigheter og expiration`() {
		val norskIdent = "1243234"

		val oldData = RettigheterService.CachetRettigheter(
			listOf(AltinnRettighet("123", "4367842"))
		)

		val newData = RettigheterService.CachetRettigheter(
			listOf(AltinnRettighet("784932", "11111111"))
		)

		val oldExpiration = ZonedDateTime.now().plusHours(1)
		val newExpiration = ZonedDateTime.now().plusHours(12)

		repository.upsertData(norskIdent, 1, toJsonString(oldData), oldExpiration)
		repository.upsertData(norskIdent, 1, toJsonString(newData), newExpiration)

		val cachetRettighet = repository.hentCachetData(norskIdent, 1)

		val expectedJson = """
			{"rettigheter":[{"organisasjonsnummer":"784932","serviceCode":"11111111"}]}
		""".trimIndent()

		cachetRettighet shouldNotBe null
		cachetRettighet!!.dataJson shouldBe expectedJson
		cachetRettighet.expiresAfter shouldBeEqualTo newExpiration
	}


	@Test
	fun `hentUtdaterteBrukere - ingen utdaterte brukere - returnerer tom liste`() {
		val personligIdent = "235435"
		val data = RettigheterService.CachetRettigheter(
			listOf(AltinnRettighet("123", "4367842"))
		)
		repository.upsertData(norskIdent = personligIdent, 1, toJsonString(data), ZonedDateTime.now().plusHours(1))

		repository.hentUtdaterteBrukere(1).size shouldBe 0
	}

	@Test
	fun `hentUtdaterteBrukere - utdatert bruker - returnerer bruker`() {
		val personligIdent = "235435"
		val personligIdent2 = "235435"

		val data = RettigheterService.CachetRettigheter(
			listOf(AltinnRettighet("123", "4367842"))
		)
		repository.upsertData(norskIdent = personligIdent, 1, toJsonString(data), ZonedDateTime.now().plusHours(1))
		repository.upsertData(norskIdent = personligIdent2, 1, toJsonString(data), ZonedDateTime.now().minusMinutes(1))

		val utdaterteBrukere = repository.hentUtdaterteBrukere(1)

		utdaterteBrukere.size shouldBe 1
		utdaterteBrukere.first().norskIdent shouldBe personligIdent2
	}
}

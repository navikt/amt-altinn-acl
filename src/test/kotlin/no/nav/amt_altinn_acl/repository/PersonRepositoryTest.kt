package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.test_util.DbTestDataUtils
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class PersonRepositoryTest {

	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val repository = PersonRepository(NamedParameterJdbcTemplate(dataSource))

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
	}

	@Test
	internal fun `getOrCreate - not exist - should create new person`() {
		val norskIdent = "123456789"

		val personDbo = repository.getOrCreate(norskIdent)

		personDbo.norskIdent shouldBe norskIdent
		personDbo.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldBe
			ZonedDateTime.of(LocalDate.of(1970, 1, 1).atStartOfDay(), ZoneId.systemDefault())
	}

	@Test
	internal fun `getOrCreate - exist - should return existing person`() {
		val norskIdent = "123456789"

		val createdPerson = repository.getOrCreate(norskIdent)
		val gottenPerson = repository.getOrCreate(norskIdent)

		createdPerson.id shouldBe gottenPerson.id
	}

	@Test
	internal fun `setSynchronized - should set last_synchronized to current time`() {
		val norskIdent = "123456789"

		val today = ZonedDateTime.of(LocalDate.now().atStartOfDay(), ZoneId.systemDefault())

		val createdPerson = repository.getOrCreate(norskIdent)
		createdPerson.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldNotBe today

		repository.setSynchronized(norskIdent)
		val updatedPerson = repository.getOrCreate(norskIdent)
		updatedPerson.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldBe today
	}
}

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

class PersonRepositoryTest {

	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val repository = PersonRepository(NamedParameterJdbcTemplate(dataSource))

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
	}

	@Test
	internal fun `create - not exist - should create new person`() {
		val norskIdent = "123456789"

		val personDbo = repository.create(norskIdent)

		personDbo.norskIdent shouldBe norskIdent
		personDbo.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldBe
			ZonedDateTime.of(LocalDate.of(1970, 1, 1).atStartOfDay(), ZoneId.systemDefault())
	}

	@Test
	internal fun `createAndSetSynchronized - not exist - should create new person and set synchronized`() {
		val norskIdent = "123456789"
		val lastSynchronized = ZonedDateTime.now().minusDays(4)

		val createdPerson = repository.createAndSetSynchronized(norskIdent, lastSynchronized)

		createdPerson.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldBe lastSynchronized.truncatedTo(ChronoUnit.DAYS)
	}

	@Test
	internal fun `setSynchronized - should set last_synchronized to current time`() {
		val norskIdent = "123456789"

		val today = ZonedDateTime.of(LocalDate.now().atStartOfDay(), ZoneId.systemDefault())

		val createdPerson = repository.create(norskIdent)
		createdPerson.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldNotBe today

		repository.setSynchronized(norskIdent)
		val updatedPerson = repository.get(norskIdent)
		updatedPerson?.lastSynchronized?.truncatedTo(ChronoUnit.DAYS) shouldBe today
	}
}

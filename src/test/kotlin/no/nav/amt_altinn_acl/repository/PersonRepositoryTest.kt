package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.test_util.RepositoryTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [PersonRepository::class])
class PersonRepositoryTest : RepositoryTestBase() {

	@Autowired
	private lateinit var personRepository: PersonRepository

	@Test
	internal fun `create - not exist - should create new person`() {
		val norskIdent = "123456789"

		val personDbo = personRepository.create(norskIdent)

		personDbo.norskIdent shouldBe norskIdent
		personDbo.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldBe
			ZonedDateTime.of(LocalDate.of(1970, 1, 1).atStartOfDay(), ZoneId.systemDefault())
	}

	@Test
	internal fun `createAndSetSynchronized - not exist - should create new person and set synchronized`() {
		val norskIdent = "123456789"
		val lastSynchronized = ZonedDateTime.now().minusDays(4)

		val createdPerson = personRepository.createAndSetSynchronized(norskIdent, lastSynchronized)

		createdPerson.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldBe lastSynchronized.truncatedTo(ChronoUnit.DAYS)
	}

	@Test
	internal fun `setSynchronized - should set last_synchronized to current time`() {
		val norskIdent = "123456789"

		val today = ZonedDateTime.of(LocalDate.now().atStartOfDay(), ZoneId.systemDefault())

		val createdPerson = personRepository.create(norskIdent)
		createdPerson.lastSynchronized.truncatedTo(ChronoUnit.DAYS) shouldNotBe today

		personRepository.setSynchronized(norskIdent)
		val updatedPerson = personRepository.get(norskIdent)
		updatedPerson?.lastSynchronized?.truncatedTo(ChronoUnit.DAYS) shouldBe today
	}
}

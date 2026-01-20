package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.testutil.RepositoryTestBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest(classes = [RolleRepository::class, PersonRepository::class])
class RolleRepositoryTest(
	private val personRepository: PersonRepository,
	private val rolleRepository: RolleRepository,
) : RepositoryTestBase() {
	private var personId: Long = Long.MIN_VALUE

	@BeforeEach
	internal fun setUp() {
		val person = personRepository.create("12345678")
		personId = person.id
	}

	@Test
	internal fun `createRolle - returns correct rolle`() {
		val organisasjonsnummer = UUID.randomUUID().toString()

		val rolle = rolleRepository.createRolle(personId, organisasjonsnummer, RolleType.VEILEDER)

		rolle.organisasjonsnummer shouldBe organisasjonsnummer
	}

	@Test
	internal fun `invalidateRolle - Sets validTo to current timestamp - does not return from getValidRules`() {
		val organisasjonsnummer = UUID.randomUUID().toString()

		val rolle = rolleRepository.createRolle(personId, organisasjonsnummer, RolleType.VEILEDER)
		rolleRepository.invalidateRolle(rolle.id)

		val gyldigeRoller =
			rolleRepository
				.hentRollerForPerson(personId)
				.filter { it.erGyldig() }

		gyldigeRoller.isEmpty() shouldBe true

		val alleRoller = rolleRepository.hentRollerForPerson(personId)
		alleRoller.size shouldBe 1
		alleRoller.first().validTo shouldNotBe null
	}
}

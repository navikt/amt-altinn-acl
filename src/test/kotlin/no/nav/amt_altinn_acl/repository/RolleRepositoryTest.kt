package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.test_util.DbTestDataUtils
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class RolleRepositoryTest {

	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val personRepository = PersonRepository(template)
	private val repository = RolleRepository(template)

	private var personId: Long = Long.MIN_VALUE

	@BeforeEach
	internal fun setUp() {
		DbTestDataUtils.cleanDatabase(dataSource)
		val person = personRepository.getOrCreate("12345678")
		personId = person.id
	}

	@Test
	internal fun `createRolle - returns correct rolle`() {
		val organizationNumber = UUID.randomUUID().toString()

		val rolle = repository.createRolle(personId, organizationNumber, RolleType.VEILEDER)

		rolle.organizationNumber shouldBe organizationNumber
	}

	@Test
	internal fun `invalidateRolle - Sets validTo to current timestamp - does not return from getValidRules`() {
		val organizationNumber = UUID.randomUUID().toString()

		val rolle = repository.createRolle(personId, organizationNumber, RolleType.VEILEDER)
		repository.invalidateRolle(rolle.id)

		val gyldigeRoller = repository.getRollerForPerson(personId)
		gyldigeRoller.isEmpty() shouldBe true

		val alleRoller = repository.getRollerForPerson(personId, false)
		alleRoller.size shouldBe 1
		alleRoller.first().validTo shouldNotBe null
	}
}

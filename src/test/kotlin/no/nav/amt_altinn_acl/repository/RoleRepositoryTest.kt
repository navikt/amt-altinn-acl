package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RoleType
import no.nav.amt_altinn_acl.test_util.DbTestDataUtils
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class RoleRepositoryTest {

	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val personRepository = PersonRepository(template)
	private val repository = RoleRepository(template)

	private var personId: Long = Long.MIN_VALUE

	@BeforeEach
	internal fun setUp() {
		DbTestDataUtils.cleanDatabase(dataSource)
		val person = personRepository.getOrCreate("12345678")
		personId = person.id
	}

	@Test
	internal fun `createRole - returns correct right`() {
		val organizationNumber = UUID.randomUUID().toString()

		val right = repository.createRole(personId, organizationNumber, RoleType.VEILEDER)

		right.organizationNumber shouldBe organizationNumber
	}

	@Test
	internal fun `invalidateRole - Sets validTo to current timestamp - does not return from getValidRules`() {
		val organizationNumber = UUID.randomUUID().toString()

		val right = repository.createRole(personId, organizationNumber, RoleType.VEILEDER)
		repository.invalidateRole(right.id)

		val validRoles = repository.getRolesForPerson(personId)
		validRoles.isEmpty() shouldBe true

		val allRoles = repository.getRolesForPerson(personId, false)
		allRoles.size shouldBe 1
		allRoles.first().validTo shouldNotBe null
	}
}

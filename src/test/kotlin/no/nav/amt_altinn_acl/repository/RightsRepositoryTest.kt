package no.nav.amt_altinn_acl.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RightType
import no.nav.amt_altinn_acl.test_util.DbTestDataUtils
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class RightsRepositoryTest {

	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val personRepository = PersonRepository(template)
	private val repository = RightsRepository(template)

	private var personId: Long = Long.MIN_VALUE

	@BeforeEach
	internal fun setUp() {
		DbTestDataUtils.cleanDatabase(dataSource)
		val person = personRepository.getOrCreate("12345678")
		personId = person.id
	}

	@Test
	internal fun `createRight - returns correct right`() {
		val organizationNumber = UUID.randomUUID().toString()

		val right = repository.createRight(personId, organizationNumber, RightType.VEILEDER)

		right.organizationNumber shouldBe organizationNumber
	}

	@Test
	internal fun `invalidateRight - Sets validTo to current timestamp - does not return from getValidRules`() {
		val organizationNumber = UUID.randomUUID().toString()

		val right = repository.createRight(personId, organizationNumber, RightType.VEILEDER)
		repository.invalidateRight(right.id)

		val validRights = repository.getRightsForPerson(personId)
		validRights.isEmpty() shouldBe true

		val allRights = repository.getRightsForPerson(personId, false)
		allRights.size shouldBe 1
		allRights.first().validTo shouldNotBe null
	}
}

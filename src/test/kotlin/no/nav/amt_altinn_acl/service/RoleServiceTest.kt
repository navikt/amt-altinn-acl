package no.nav.amt_altinn_acl.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RoleType
import no.nav.amt_altinn_acl.domain.RoleType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RoleType.VEILEDER
import no.nav.amt_altinn_acl.domain.RolesOnOrganization
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RoleRepository
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class RoleServiceTest : IntegrationTest() {

	@Autowired
	lateinit var roleService: RoleService

	@Autowired
	lateinit var personRepository: PersonRepository

	@Autowired
	lateinit var roleRepository: RoleRepository

	@BeforeEach
	internal fun setUp() {
		mockMaskinportenHttpClient.enqueueTokenResponse()
		mockAltinnHttpClient.resetHttpServer()
	}

	@Test
	internal fun `getRightsForPerson - not exist - create person and get rights from altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))

		val rights = roleService.getRolesForPerson(norskIdent)

		mockAltinnHttpClient.requestCount() shouldBe 2

		rights.size shouldBe 1

		hasRight(rights, organization, VEILEDER) shouldBe true
		hasRight(rights, organization, KOORDINATOR) shouldBe true

		val databasePerson = personRepository.getOrCreate(norskIdent)
		databasePerson.lastSynchronized.days() shouldBe ZonedDateTime.now().days()

		hasRightInDatabase(databasePerson.id, organization, VEILEDER) shouldBe true
		hasRightInDatabase(databasePerson.id, organization, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRightsForPerson - exists - has rights - return cached rights if under cacheTime`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		personRepository.setSynchronized(norskIdent)

		roleRepository.createRole(
			personId = personDbo.id,
			organizationNumber = organization,
			roleType = KOORDINATOR
		)

		roleService.getRolesForPerson(norskIdent)
		mockAltinnHttpClient.requestCount() shouldBe 0
	}

	@Test
	internal fun `getRightsForPerson - exists - has no rights - should check altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		personRepository.getOrCreate(norskIdent)
		personRepository.setSynchronized(norskIdent)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))

		val rights = roleService.getRolesForPerson(norskIdent)

		hasRight(rights, organization, VEILEDER) shouldBe true
		hasRight(rights, organization, KOORDINATOR) shouldBe true

		mockAltinnHttpClient.requestCount() shouldBe 2
	}

	@Test
	internal fun `getRightsForPerson - exists - has lost rights in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		roleRepository.createRole(personDbo.id, organization, KOORDINATOR)
		roleRepository.createRole(personDbo.id, organization, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val rights = roleService.getRolesForPerson(norskIdent)

		hasRight(rights, organization, VEILEDER) shouldBe false
		hasRight(rights, organization, KOORDINATOR) shouldBe true

		val invalidVeileder = roleRepository.getRolesForPerson(personDbo.id, false).find { it.roleType == VEILEDER }!!

		invalidVeileder.validTo shouldNotBe null
	}

	@Test
	internal fun `getRightsForPerson - exists - has gained rights in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		roleRepository.createRole(personDbo.id, organization, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))

		val rights = roleService.getRolesForPerson(norskIdent)

		hasRight(rights, organization, VEILEDER) shouldBe true
		hasRight(rights, organization, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRightsForPerson - exists - has regained right in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		roleRepository.createRole(personDbo.id, organization, KOORDINATOR)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf())
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val rights = roleService.getRolesForPerson(norskIdent)

		rights.isEmpty() shouldBe true

		mockAltinnHttpClient.resetHttpServer()
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val updatedRights = roleService.getRolesForPerson(norskIdent)

		hasRight(updatedRights, organization, KOORDINATOR) shouldBe true

		val databaseRights = roleRepository.getRolesForPerson(personDbo.id, false)
			.filter { it.roleType == KOORDINATOR }

		databaseRights.size shouldBe 2

	}

	private fun hasRightInDatabase(personId: Long, organizationNumber: String, right: RoleType): Boolean {
		return roleRepository.getRolesForPerson(personId)
			.find { it.organizationNumber == organizationNumber && it.roleType == right } != null
	}

	private fun hasRight(list: List<RolesOnOrganization>, organizationNumber: String, right: RoleType): Boolean {
		return list.find { it.organizationNumber == organizationNumber }
			?.roles?.find { it.roleType == right } != null
	}

	private fun ZonedDateTime.days(): ZonedDateTime {
		return this.truncatedTo(ChronoUnit.DAYS)
	}
}

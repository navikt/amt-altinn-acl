package no.nav.amt_altinn_acl.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.domain.RolleType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RolleType.VEILEDER
import no.nav.amt_altinn_acl.domain.RollerInOrganization
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RolleRepository
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class RolleServiceTest : IntegrationTest() {

	@Autowired
	lateinit var rolleService: RolleService

	@Autowired
	lateinit var personRepository: PersonRepository

	@Autowired
	lateinit var rolleRepository: RolleRepository

	@BeforeEach
	internal fun setUp() {
		mockMaskinportenHttpClient.enqueueTokenResponse()
		mockAltinnHttpClient.resetHttpServer()
	}

	@Test
	internal fun `getRollerForPerson - not exist - create person and get roller from altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))

		val roller = rolleService.getRollerForPerson(norskIdent)

		mockAltinnHttpClient.requestCount() shouldBe 2

		roller.size shouldBe 1

		hasRolle(roller, organization, VEILEDER) shouldBe true
		hasRolle(roller, organization, KOORDINATOR) shouldBe true

		val databasePerson = personRepository.getOrCreate(norskIdent)
		databasePerson.lastSynchronized.days() shouldBe ZonedDateTime.now().days()

		hasRolleInDatabase(databasePerson.id, organization, VEILEDER) shouldBe true
		hasRolleInDatabase(databasePerson.id, organization, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRollerForPerson - exists - has rolle - return cached rolle if under cacheTime`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		personRepository.setSynchronized(norskIdent)

		rolleRepository.createRolle(
			personId = personDbo.id,
			organizationNumber = organization,
			rolleType = KOORDINATOR
		)

		rolleService.getRollerForPerson(norskIdent)
		mockAltinnHttpClient.requestCount() shouldBe 0
	}

	@Test
	internal fun `getRollerForPerson - exists - has no roller - should check altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		personRepository.getOrCreate(norskIdent)
		personRepository.setSynchronized(norskIdent)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))

		val roller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(roller, organization, VEILEDER) shouldBe true
		hasRolle(roller, organization, KOORDINATOR) shouldBe true

		mockAltinnHttpClient.requestCount() shouldBe 2
	}

	@Test
	internal fun `getRollerForPerson - exists - has lost roller in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rolleRepository.createRolle(personDbo.id, organization, KOORDINATOR)
		rolleRepository.createRolle(personDbo.id, organization, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val roller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(roller, organization, VEILEDER) shouldBe false
		hasRolle(roller, organization, KOORDINATOR) shouldBe true

		val invalidVeileder = rolleRepository.getRollerForPerson(personDbo.id, false).find { it.rolleType == VEILEDER }!!

		invalidVeileder.validTo shouldNotBe null
	}

	@Test
	internal fun `getRollerForPerson - exists - has gained rolle in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rolleRepository.createRolle(personDbo.id, organization, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))

		val roller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(roller, organization, VEILEDER) shouldBe true
		hasRolle(roller, organization, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRollerForPerson - exists - has regained rolle in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rolleRepository.createRolle(personDbo.id, organization, KOORDINATOR)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf())
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val roller = rolleService.getRollerForPerson(norskIdent)

		roller.isEmpty() shouldBe true

		mockAltinnHttpClient.resetHttpServer()
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val updatedRoller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(updatedRoller, organization, KOORDINATOR) shouldBe true

		val databaseRoller = rolleRepository.getRollerForPerson(personDbo.id, false)
			.filter { it.rolleType == KOORDINATOR }

		databaseRoller.size shouldBe 2

	}

	private fun hasRolleInDatabase(personId: Long, organizationNumber: String, rolle: RolleType): Boolean {
		return rolleRepository.getRollerForPerson(personId)
			.find { it.organizationNumber == organizationNumber && it.rolleType == rolle } != null
	}

	private fun hasRolle(list: List<RollerInOrganization>, organizationNumber: String, rolle: RolleType): Boolean {
		return list.find { it.organizationNumber == organizationNumber }
			?.roller?.find { it.rolleType == rolle } != null
	}

	private fun ZonedDateTime.days(): ZonedDateTime {
		return this.truncatedTo(ChronoUnit.DAYS)
	}
}

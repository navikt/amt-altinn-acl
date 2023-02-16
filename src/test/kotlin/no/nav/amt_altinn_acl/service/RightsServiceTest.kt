package no.nav.amt_altinn_acl.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RightType
import no.nav.amt_altinn_acl.domain.RightType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RightType.VEILEDER
import no.nav.amt_altinn_acl.domain.RightsOnOrganization
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RightsRepository
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class RightsServiceTest : IntegrationTest() {

	@Autowired
	lateinit var rightsService: RightsService

	@Autowired
	lateinit var personRepository: PersonRepository

	@Autowired
	lateinit var rightsRepository: RightsRepository

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

		val rights = rightsService.getRightsForPerson(norskIdent)

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

		rightsRepository.createRight(
			personId = personDbo.id,
			organizationNumber = organization,
			rightType = KOORDINATOR
		)

		rightsService.getRightsForPerson(norskIdent)
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

		val rights = rightsService.getRightsForPerson(norskIdent)

		hasRight(rights, organization, VEILEDER) shouldBe true
		hasRight(rights, organization, KOORDINATOR) shouldBe true

		mockAltinnHttpClient.requestCount() shouldBe 2
	}

	@Test
	internal fun `getRightsForPerson - exists - has lost rights in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rightsRepository.createRight(personDbo.id, organization, KOORDINATOR)
		rightsRepository.createRight(personDbo.id, organization, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val rights = rightsService.getRightsForPerson(norskIdent)

		hasRight(rights, organization, VEILEDER) shouldBe false
		hasRight(rights, organization, KOORDINATOR) shouldBe true

		val invalidVeileder = rightsRepository.getRightsForPerson(personDbo.id, false).find { it.rightType == VEILEDER }!!

		invalidVeileder.validTo shouldNotBe null
	}

	@Test
	internal fun `getRightsForPerson - exists - has gained rights in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rightsRepository.createRight(personDbo.id, organization, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organization))

		val rights = rightsService.getRightsForPerson(norskIdent)

		hasRight(rights, organization, VEILEDER) shouldBe true
		hasRight(rights, organization, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRightsForPerson - exists - has regained right in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organization = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rightsRepository.createRight(personDbo.id, organization, KOORDINATOR)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf())
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val rights = rightsService.getRightsForPerson(norskIdent)

		rights.isEmpty() shouldBe true

		mockAltinnHttpClient.resetHttpServer()
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organization))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val updatedRights = rightsService.getRightsForPerson(norskIdent)

		hasRight(updatedRights, organization, KOORDINATOR) shouldBe true

		val databaseRights = rightsRepository.getRightsForPerson(personDbo.id, false)
			.filter { it.rightType == KOORDINATOR }

		databaseRights.size shouldBe 2

	}

	private fun hasRightInDatabase(personId: Long, organizationNumber: String, right: RightType): Boolean {
		return rightsRepository.getRightsForPerson(personId)
			.find { it.organizationNumber == organizationNumber && it.rightType == right } != null
	}

	private fun hasRight(list: List<RightsOnOrganization>, organizationNumber: String, right: RightType): Boolean {
		return list.find { it.organizationNumber == organizationNumber }
			?.rights?.find { it.rightType == right } != null
	}

	private fun ZonedDateTime.days(): ZonedDateTime {
		return this.truncatedTo(ChronoUnit.DAYS)
	}
}

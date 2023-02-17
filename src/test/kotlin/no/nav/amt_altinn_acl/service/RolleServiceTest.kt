package no.nav.amt_altinn_acl.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.domain.RolleType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RolleType.VEILEDER
import no.nav.amt_altinn_acl.domain.RollerIOrganisasjon
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
		val organisasjonsnummer = UUID.randomUUID().toString()

		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organisasjonsnummer))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organisasjonsnummer))

		val roller = rolleService.getRollerForPerson(norskIdent)

		mockAltinnHttpClient.requestCount() shouldBe 2

		roller.size shouldBe 1

		hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe true
		hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true

		val databasePerson = personRepository.getOrCreate(norskIdent)
		databasePerson.lastSynchronized.days() shouldBe ZonedDateTime.now().days()

		hasRolleInDatabase(databasePerson.id, organisasjonsnummer, VEILEDER) shouldBe true
		hasRolleInDatabase(databasePerson.id, organisasjonsnummer, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRollerForPerson - exists - has rolle - return cached rolle if under cacheTime`() {
		val norskIdent = UUID.randomUUID().toString()
		val organisasjonsnummer = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		personRepository.setSynchronized(norskIdent)

		rolleRepository.createRolle(
			personId = personDbo.id,
			organisasjonsnummer = organisasjonsnummer,
			rolleType = KOORDINATOR
		)

		rolleService.getRollerForPerson(norskIdent)
		mockAltinnHttpClient.requestCount() shouldBe 0
	}

	@Test
	internal fun `getRollerForPerson - exists - has no roller - should check altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organisasjonsnummer = UUID.randomUUID().toString()

		personRepository.getOrCreate(norskIdent)
		personRepository.setSynchronized(norskIdent)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organisasjonsnummer))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organisasjonsnummer))

		val roller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe true
		hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true

		mockAltinnHttpClient.requestCount() shouldBe 2
	}

	@Test
	internal fun `getRollerForPerson - exists - has lost roller in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organisasjonsnummer = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rolleRepository.createRolle(personDbo.id, organisasjonsnummer, KOORDINATOR)
		rolleRepository.createRolle(personDbo.id, organisasjonsnummer, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organisasjonsnummer))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val roller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe false
		hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true

		val invalidVeileder = rolleRepository.hentRollerForPerson(personDbo.id).find { it.rolleType == VEILEDER }!!

		invalidVeileder.validTo shouldNotBe null
	}

	@Test
	internal fun `getRollerForPerson - exists - has gained rolle in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organisasjonsnummer = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rolleRepository.createRolle(personDbo.id, organisasjonsnummer, VEILEDER)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organisasjonsnummer))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf(organisasjonsnummer))

		val roller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe true
		hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true
	}

	@Test
	internal fun `getRollerForPerson - exists - has regained rolle in altinn`() {
		val norskIdent = UUID.randomUUID().toString()
		val organisasjonsnummer = UUID.randomUUID().toString()

		val personDbo = personRepository.getOrCreate(norskIdent)
		rolleRepository.createRolle(personDbo.id, organisasjonsnummer, KOORDINATOR)

		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf())
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val roller = rolleService.getRollerForPerson(norskIdent)

		roller.isEmpty() shouldBe true

		mockAltinnHttpClient.resetHttpServer()
		mockAltinnHttpClient.addReporteeResponse(norskIdent, KOORDINATOR.serviceCode, listOf(organisasjonsnummer))
		mockAltinnHttpClient.addReporteeResponse(norskIdent, VEILEDER.serviceCode, listOf())

		val updatedRoller = rolleService.getRollerForPerson(norskIdent)

		hasRolle(updatedRoller, organisasjonsnummer, KOORDINATOR) shouldBe true

		val databaseRoller = rolleRepository.hentRollerForPerson(personDbo.id)
			.filter { it.rolleType == KOORDINATOR }

		databaseRoller.size shouldBe 2

	}

	@Test
	internal fun `getRollerForPerson - Altinn down - returns cached roller`() {
		val norskIdent = UUID.randomUUID().toString()
		val organisasjonsnummer = UUID.randomUUID().toString()
		val personDbo = personRepository.getOrCreate(norskIdent)

		rolleRepository.createRolle(personDbo.id, organisasjonsnummer, KOORDINATOR)

		mockAltinnHttpClient.addFailureResponse(norskIdent, KOORDINATOR.serviceCode, 500)
		mockAltinnHttpClient.addFailureResponse(norskIdent, VEILEDER.serviceCode, 500)

		val roller = rolleService.getRollerForPerson(norskIdent)

		mockAltinnHttpClient.requestCount() shouldBe 1
		hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true

		val updatedPersonDbo = personRepository.getOrCreate(norskIdent)

		updatedPersonDbo.lastSynchronized.days() shouldNotBe ZonedDateTime.now().days()
	}

	private fun hasRolleInDatabase(personId: Long, organisasjonsnummerNumber: String, rolle: RolleType): Boolean {
		return rolleRepository.hentRollerForPerson(personId)
			.filter { it.erGyldig() }
			.find { it.organisasjonsnummer == organisasjonsnummerNumber && it.rolleType == rolle } != null
	}

	private fun hasRolle(list: List<RollerIOrganisasjon>, organisasjonsnummerNumber: String, rolle: RolleType): Boolean {
		return list.find { it.organisasjonsnummer == organisasjonsnummerNumber }
			?.roller?.find { it.rolleType == rolle } != null
	}

	private fun ZonedDateTime.days(): ZonedDateTime {
		return this.truncatedTo(ChronoUnit.DAYS)
	}
}

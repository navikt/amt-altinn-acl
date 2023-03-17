package no.nav.amt_altinn_acl

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.domain.RolleType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RolleType.VEILEDER
import no.nav.amt_altinn_acl.domain.RollerIOrganisasjon
import no.nav.amt_altinn_acl.jobs.AltinnUpdater
import no.nav.amt_altinn_acl.jobs.leaderelection.LeaderElection
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RolleRepository
import no.nav.amt_altinn_acl.service.RolleService
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.random.Random

class AltinnUpdaterTests {
	private lateinit var personRepository: PersonRepository
	private lateinit var rolleRepository: RolleRepository

	private lateinit var rolleService: RolleService

	private lateinit var altinnUpdater: AltinnUpdater
	private lateinit var altinnClient: AltinnClient
	private lateinit var leaderElection: LeaderElection
	private val dataSource = SingletonPostgresContainer.getDataSource()

	@BeforeEach
	fun setup() {
		altinnClient = mockk()
		leaderElection = mockk()
		every { leaderElection.isLeader() } returns true

		val template = NamedParameterJdbcTemplate(dataSource)
		personRepository = PersonRepository(template)
		rolleRepository = RolleRepository(template)

		rolleService = RolleService(personRepository, rolleRepository, altinnClient)

		altinnUpdater = AltinnUpdater(rolleService, leaderElection)
	}

	@Test
	fun `update - utdatert bruker - skal synkronisere bruker`() {
		val organisasjonsnummer = "2131"
		val personligIdent = Random.nextLong().toString()

		personRepository.create(personligIdent)

		every {
			altinnClient.hentOrganisasjoner(personligIdent, KOORDINATOR.serviceCode)
		} returns Result.success(listOf(organisasjonsnummer))


		every {
			altinnClient.hentOrganisasjoner(personligIdent, VEILEDER.serviceCode)
		} returns Result.success(emptyList())

		altinnUpdater.update()

		val oppdaterteRettigheter = rolleService.getRollerForPerson(personligIdent)
		hasRolle(oppdaterteRettigheter, organisasjonsnummer, KOORDINATOR) shouldBe true
	}

	private fun hasRolle(list: List<RollerIOrganisasjon>, organizationNumber: String, rolle: RolleType): Boolean {
		return list.find { it.organisasjonsnummer == organizationNumber }
			?.roller?.find { it.rolleType == rolle } != null
	}



}

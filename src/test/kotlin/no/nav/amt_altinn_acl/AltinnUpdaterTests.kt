package no.nav.amt_altinn_acl

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.domain.RolleType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RolleType.VEILEDER
import no.nav.amt_altinn_acl.domain.RollerIOrganisasjon
import no.nav.amt_altinn_acl.jobs.AltinnUpdater
import no.nav.amt_altinn_acl.jobs.leaderelection.LeaderElection
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.service.RolleService
import no.nav.amt_altinn_acl.test_util.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random


class AltinnUpdaterTests(
	private val personRepository: PersonRepository,
	private val rolleService: RolleService,
	private val altinnUpdater: AltinnUpdater,
	@MockkBean private val altinnClient: AltinnClient,
	@MockkBean private val leaderElection: LeaderElection
) : IntegrationTest() {

	@BeforeEach
	fun setup() {
		every { leaderElection.isLeader() } returns true
	}

	@Test
	fun `update - utdatert bruker - skal synkronisere bruker`() {
		val organisasjonsnummer = "2131"
		val personligIdent = Random.nextLong().toString()

		personRepository.create(personligIdent)

		every {
			altinnClient.hentRoller(personligIdent, RolleType.entries)
		} returns mapOf(KOORDINATOR to listOf(organisasjonsnummer), VEILEDER to emptyList())


		altinnUpdater.update()

		val oppdaterteRettigheter = rolleService.getRollerForPerson(personligIdent)
		hasRolle(oppdaterteRettigheter, organisasjonsnummer, KOORDINATOR) shouldBe true
	}

	private fun hasRolle(list: List<RollerIOrganisasjon>, organizationNumber: String, rolle: RolleType): Boolean =
		list.find { it.organisasjonsnummer == organizationNumber }
			?.roller?.find { it.rolleType == rolle } != null
}

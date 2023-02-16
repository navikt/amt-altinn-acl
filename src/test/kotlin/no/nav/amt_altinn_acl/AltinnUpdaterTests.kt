package no.nav.amt_altinn_acl

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.RoleType
import no.nav.amt_altinn_acl.domain.RoleType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RoleType.VEILEDER
import no.nav.amt_altinn_acl.domain.RolesOnOrganization
import no.nav.amt_altinn_acl.jobs.AltinnUpdater
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RoleRepository
import no.nav.amt_altinn_acl.service.RoleService
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.random.Random

class AltinnUpdaterTests {
	private lateinit var personRepository: PersonRepository
	private lateinit var rightRepository: RoleRepository

	private lateinit var rightService: RoleService

	private lateinit var altinnUpdater: AltinnUpdater
	private lateinit var altinnClient: AltinnClient
	private val dataSource = SingletonPostgresContainer.getDataSource()

	@BeforeEach
	fun setup() {
		altinnClient = mockk()

		val template = NamedParameterJdbcTemplate(dataSource)
		personRepository = PersonRepository(template)
		rightRepository = RoleRepository(template)

		rightService = RoleService(personRepository, rightRepository, altinnClient)

		altinnUpdater = AltinnUpdater(rightService)
	}

	@Test
	fun `update - utdatert bruker - skal synkronisere bruker`() {
		val organisasjonsnummer = "2131"
		val personligIdent = Random.nextLong().toString()

		personRepository.getOrCreate(personligIdent)

		every {
			altinnClient.hentOrganisasjoner(personligIdent, KOORDINATOR.serviceCode)
		} returns Result.success(listOf(organisasjonsnummer))


		every {
			altinnClient.hentOrganisasjoner(personligIdent, VEILEDER.serviceCode)
		} returns Result.success(emptyList())

		altinnUpdater.update()

		val oppdaterteRettigheter = rightService.getRolesForPerson(personligIdent)
		hasRight(oppdaterteRettigheter, organisasjonsnummer, KOORDINATOR) shouldBe true
	}

	private fun hasRight(list: List<RolesOnOrganization>, organizationNumber: String, right: RoleType): Boolean {
		return list.find { it.organizationNumber == organizationNumber }
			?.roles?.find { it.roleType == right } != null
	}



}

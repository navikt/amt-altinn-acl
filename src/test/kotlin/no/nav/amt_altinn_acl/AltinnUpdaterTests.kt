package no.nav.amt_altinn_acl

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.RightType
import no.nav.amt_altinn_acl.domain.RightType.KOORDINATOR
import no.nav.amt_altinn_acl.domain.RightType.VEILEDER
import no.nav.amt_altinn_acl.domain.RightsOnOrganization
import no.nav.amt_altinn_acl.jobs.AltinnUpdater
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RightsRepository
import no.nav.amt_altinn_acl.service.RettigheterService
import no.nav.amt_altinn_acl.service.RightsService
import no.nav.amt_altinn_acl.test_util.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.random.Random

class AltinnUpdaterTests {
	private lateinit var personRepository: PersonRepository
	private lateinit var rightRepository: RightsRepository

	private lateinit var rightService: RightsService

	private lateinit var altinnUpdater: AltinnUpdater
	private lateinit var altinnClient: AltinnClient
	private val dataSource = SingletonPostgresContainer.getDataSource()

	@BeforeEach
	fun setup() {
		altinnClient = mockk()

		val template = NamedParameterJdbcTemplate(dataSource)
		personRepository = PersonRepository(template)
		rightRepository = RightsRepository(template)

		rightService = RightsService(personRepository, rightRepository, altinnClient)

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

		val oppdaterteRettigheter = rightService.getRightsForPerson(personligIdent)
		hasRight(oppdaterteRettigheter, organisasjonsnummer, KOORDINATOR) shouldBe true
	}

	private fun hasRight(list: List<RightsOnOrganization>, organizationNumber: String, right: RightType): Boolean {
		return list.find { it.organizationNumber == organizationNumber }
			?.rights?.find { it.rightType == right } != null
	}



}

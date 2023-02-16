package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.Right
import no.nav.amt_altinn_acl.domain.RightType
import no.nav.amt_altinn_acl.domain.RightsOnOrganization
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RightsRepository
import no.nav.amt_altinn_acl.repository.dbo.RightDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class RightsService(
	private val personRepository: PersonRepository,
	private val rightsRepository: RightsRepository,
	private val altinnClient: AltinnClient
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun getRightsForPerson(norskIdent: String, onlyValid: Boolean = true): List<RightsOnOrganization> {
		val person = personRepository.getOrCreate(norskIdent)
		val synchronizeIfBefore = ZonedDateTime.now().minusHours(1)

		val rights = if (person.lastSynchronized.isBefore(synchronizeIfBefore)) {
			updateRights(person.id, norskIdent)
			rightsRepository.getRightsForPerson(person.id, onlyValid)
		} else {
			rightsRepository.getRightsForPerson(person.id, onlyValid).let {
				if (it.isEmpty()) {
					updateRights(person.id, norskIdent)
					return@let rightsRepository.getRightsForPerson(person.id, onlyValid)
				}
				return@let it
			}

		}

		return map(rights)
	}

	fun synchronizeUsers(max: Int = 25, synchronizedBefore: LocalDateTime = LocalDateTime.now().minusWeeks(1)) {
		val personsToSynchronize = personRepository.getUnsynchronizedPersons(max, synchronizedBefore)

		log.info("Starter synkronisering av ${personsToSynchronize.size} brukere med utgått tilgang")

		personsToSynchronize.forEach { personDbo ->
			updateRights(personDbo.id, personDbo.norskIdent)
		}

		log.info("Fullført synkronisering av ${personsToSynchronize.size} brukere med utgått tilgang")
	}

	private fun updateRights(id: Long, norskIdent: String) {
		val start = Instant.now()

		val allOldRights = rightsRepository.getRightsForPerson(id)

		RightType.values().forEach { right ->
			val oldRights = allOldRights.filter { it.rightType == right }

			val oranizationsWithRight = altinnClient.hentOrganisasjoner(norskIdent, right.serviceCode)
				.getOrThrow()

			oldRights.forEach { oldRight ->
				if (!oranizationsWithRight.contains(oldRight.organizationNumber)) {
					rightsRepository.invalidateRight(oldRight.id)
				}
			}

			oranizationsWithRight.forEach { orgRight ->
				if (oldRights.find { it.organizationNumber == orgRight } == null) {
					rightsRepository.createRight(id, orgRight, right)
				}
			}
		}

		personRepository.setSynchronized(norskIdent)
		val duration = Duration.between(start, Instant.now())
		log.info("Updated rights for person with id $id in ${duration.toMillis()} ms")
	}

	private fun map(rights: List<RightDbo>): List<RightsOnOrganization> {
		val rightsPerOrganization = rights.associateBy(
			{ it.organizationNumber },
			{ rights.filter { r -> r.organizationNumber == it.organizationNumber } })

		return rightsPerOrganization.map { org ->
			RightsOnOrganization(
				organizationNumber = org.key,
				rights = org.value.map { right ->
					Right(
						id = right.id,
						rightType = right.rightType,
						validFrom = right.validFrom,
						validTo = right.validTo
					)
				}
			)
		}
	}

}

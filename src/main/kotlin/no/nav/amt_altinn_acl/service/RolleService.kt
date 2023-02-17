package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.Rolle
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.domain.RollerInOrganization
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RolleRepository
import no.nav.amt_altinn_acl.repository.dbo.RolleDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class RolleService(
	private val personRepository: PersonRepository,
	private val rolleRepository: RolleRepository,
	private val altinnClient: AltinnClient
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun getRollerForPerson(norskIdent: String, onlyValid: Boolean = true): List<RollerInOrganization> {
		val person = personRepository.getOrCreate(norskIdent)
		val synchronizeIfBefore = ZonedDateTime.now().minusHours(1)

		val rights = if (person.lastSynchronized.isBefore(synchronizeIfBefore)) {
			updateRoller(person.id, norskIdent)
			rolleRepository.getRollerForPerson(person.id, onlyValid)
		} else {
			rolleRepository.getRollerForPerson(person.id, onlyValid).let {
				if (it.isEmpty()) {
					updateRoller(person.id, norskIdent)
					return@let rolleRepository.getRollerForPerson(person.id, onlyValid)
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
			updateRoller(personDbo.id, personDbo.norskIdent)
		}

		log.info("Fullført synkronisering av ${personsToSynchronize.size} brukere med utgått tilgang")
	}

	private fun updateRoller(id: Long, norskIdent: String) {
		val start = Instant.now()

		val allOldRights = rolleRepository.getRollerForPerson(id)

		RolleType.values().forEach { right ->
			val oldRights = allOldRights.filter { it.rolleType == right }

			val oranizationsWithRight = altinnClient.hentOrganisasjoner(norskIdent, right.serviceCode)
				.getOrThrow()

			oldRights.forEach { oldRight ->
				if (!oranizationsWithRight.contains(oldRight.organizationNumber)) {
					log.debug("User $id lost $right on ${oldRight.organizationNumber}")
					rolleRepository.invalidateRolle(oldRight.id)
				}
			}

			oranizationsWithRight.forEach { orgRight ->
				if (oldRights.find { it.organizationNumber == orgRight } == null) {
					log.debug("User $id got $right on $orgRight")
					rolleRepository.createRolle(id, orgRight, right)
				}
			}
		}

		personRepository.setSynchronized(norskIdent)
		val duration = Duration.between(start, Instant.now())
		log.info("Updated rights for person with id $id in ${duration.toMillis()} ms")
	}

	private fun map(rights: List<RolleDbo>): List<RollerInOrganization> {
		val rollerPerOrganization = rights.associateBy(
			{ it.organizationNumber },
			{ rights.filter { r -> r.organizationNumber == it.organizationNumber } })

		return rollerPerOrganization.map { org ->
			RollerInOrganization(
				organizationNumber = org.key,
				roller = org.value.map { rolle ->
					Rolle(
						id = rolle.id,
						rolleType = rolle.rolleType,
						validFrom = rolle.validFrom,
						validTo = rolle.validTo
					)
				}
			)
		}
	}

}

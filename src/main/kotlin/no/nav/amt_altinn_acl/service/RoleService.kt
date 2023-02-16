package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.Role
import no.nav.amt_altinn_acl.domain.RoleType
import no.nav.amt_altinn_acl.domain.RolesOnOrganization
import no.nav.amt_altinn_acl.repository.PersonRepository
import no.nav.amt_altinn_acl.repository.RoleRepository
import no.nav.amt_altinn_acl.repository.dbo.RoleDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class RoleService(
	private val personRepository: PersonRepository,
	private val roleRepository: RoleRepository,
	private val altinnClient: AltinnClient
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun getRolesForPerson(norskIdent: String, onlyValid: Boolean = true): List<RolesOnOrganization> {
		val person = personRepository.getOrCreate(norskIdent)
		val synchronizeIfBefore = ZonedDateTime.now().minusHours(1)

		val rights = if (person.lastSynchronized.isBefore(synchronizeIfBefore)) {
			updateRights(person.id, norskIdent)
			roleRepository.getRolesForPerson(person.id, onlyValid)
		} else {
			roleRepository.getRolesForPerson(person.id, onlyValid).let {
				if (it.isEmpty()) {
					updateRights(person.id, norskIdent)
					return@let roleRepository.getRolesForPerson(person.id, onlyValid)
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

		val allOldRights = roleRepository.getRolesForPerson(id)

		RoleType.values().forEach { right ->
			val oldRights = allOldRights.filter { it.roleType == right }

			val oranizationsWithRight = altinnClient.hentOrganisasjoner(norskIdent, right.serviceCode)
				.getOrThrow()

			oldRights.forEach { oldRight ->
				if (!oranizationsWithRight.contains(oldRight.organizationNumber)) {
					log.debug("User $id lost $right on ${oldRight.organizationNumber}")
					roleRepository.invalidateRole(oldRight.id)
				}
			}

			oranizationsWithRight.forEach { orgRight ->
				if (oldRights.find { it.organizationNumber == orgRight } == null) {
					log.debug("User $id got $right on $orgRight")
					roleRepository.createRole(id, orgRight, right)
				}
			}
		}

		personRepository.setSynchronized(norskIdent)
		val duration = Duration.between(start, Instant.now())
		log.info("Updated rights for person with id $id in ${duration.toMillis()} ms")
	}

	private fun map(rights: List<RoleDbo>): List<RolesOnOrganization> {
		val rightsPerOrganization = rights.associateBy(
			{ it.organizationNumber },
			{ rights.filter { r -> r.organizationNumber == it.organizationNumber } })

		return rightsPerOrganization.map { org ->
			RolesOnOrganization(
				organizationNumber = org.key,
				roles = org.value.map { right ->
					Role(
						id = right.id,
						roleType = right.roleType,
						validFrom = right.validFrom,
						validTo = right.validTo
					)
				}
			)
		}
	}

}

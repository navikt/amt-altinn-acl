package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.domain.Rolle
import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.domain.RollerIOrganisasjon
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

	fun getRollerForPerson(norskIdent: String): List<RollerIOrganisasjon> {
		val person = personRepository.get(norskIdent)
		val synchronizeIfBefore = ZonedDateTime.now().minusHours(1)

		if (person == null) {
			val roller = getAndSaveRollerFromAltinn(norskIdent)
			return map(roller)
		} else if (person.lastSynchronized.isBefore(synchronizeIfBefore)) {
			updateRollerFromAltinn(person.id, norskIdent)
			return map(getGyldigeRoller(norskIdent))
		} else {
			val roller = getGyldigeRoller(norskIdent).let {
				if (it.isEmpty()) {
					updateRollerFromAltinn(person.id, norskIdent)
					return@let getGyldigeRoller(norskIdent)
				}
				return@let it
			}
			return map(roller)
		}
	}

	fun synchronizeUsers(max: Int = 25, synchronizedBefore: LocalDateTime = LocalDateTime.now().minusWeeks(1)) {
		val personsToSynchronize = personRepository.getUnsynchronizedPersons(max, synchronizedBefore)

		log.info("Starter synkronisering av ${personsToSynchronize.size} brukere med utgått tilgang")

		personsToSynchronize.forEach { personDbo ->
			updateRollerFromAltinn(personDbo.id, personDbo.norskIdent)
		}

		log.info("Fullført synkronisering av ${personsToSynchronize.size} brukere med utgått tilgang")
	}

	private fun getAndSaveRollerFromAltinn(norskIdent: String): List<RolleDbo> {
		val start = Instant.now()

		val rolleMap: Map<RolleType, List<String>> = RolleType.values().associateWith { rolle ->
			val organisasjonerMedRolle = altinnClient.hentOrganisasjoner(norskIdent, rolle.serviceCode)
				.getOrElse {
					log.warn("Klarte ikke hente rolle $rolle for ny bruker", it)
					return emptyList()
				}
			organisasjonerMedRolle
		}.filterValues { it.isNotEmpty() }

		if (rolleMap.isEmpty()) {
			log.info("Bruker har ingen tilganger i Altinn")
			return emptyList()
		}

		val person = personRepository.createAndSetSynchronized(norskIdent)

		rolleMap.forEach {
			it.value.forEach { orgnummer ->
				rolleRepository.createRolle(person.id, orgnummer, it.key)
			}
		}
		val duration = Duration.between(start, Instant.now())
		log.info("Saved roller for person with id ${person.id} in ${duration.toMillis()} ms")

		return getGyldigeRoller(norskIdent)
	}

	private fun updateRollerFromAltinn(id: Long, norskIdent: String) {
		val start = Instant.now()

		val allOldRoller = getGyldigeRoller(norskIdent)

		RolleType.values().forEach { rolle ->
			val organisasjonerMedRolle = altinnClient.hentOrganisasjoner(norskIdent, rolle.serviceCode)
				.getOrElse {
					log.warn("Klarte ikke oppdatere roller for bruker $id og roller $rolle, bruker lagrede roller om eksisterer", it)
					return
				}

			val oldRoller = allOldRoller.filter { it.rolleType == rolle }

			oldRoller.forEach { oldRolle ->
				if (!organisasjonerMedRolle.contains(oldRolle.organisasjonsnummer)) {
					log.debug("User $id lost $rolle on ${oldRolle.organisasjonsnummer}")
					rolleRepository.invalidateRolle(oldRolle.id)
				}
			}

			organisasjonerMedRolle.forEach { orgRolle ->
				if (oldRoller.find { it.organisasjonsnummer == orgRolle } == null) {
					log.debug("User $id got $rolle on $orgRolle")
					rolleRepository.createRolle(id, orgRolle, rolle)
				}
			}
		}

		personRepository.setSynchronized(norskIdent)
		val duration = Duration.between(start, Instant.now())
		log.info("Updated roller for person with id $id in ${duration.toMillis()} ms")
	}

	private fun getGyldigeRoller(norskIdent: String) =
		rolleRepository.hentRollerForPerson(norskIdent)
			.filter { it.erGyldig() }


	private fun map(roller: List<RolleDbo>): List<RollerIOrganisasjon> {
		val rollerPerOrganisasjon = roller.associateBy(
			{ it.organisasjonsnummer },
			{ roller.filter { r -> r.organisasjonsnummer == it.organisasjonsnummer } })

		return rollerPerOrganisasjon.map { org ->
			RollerIOrganisasjon(
				organisasjonsnummer = org.key,
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

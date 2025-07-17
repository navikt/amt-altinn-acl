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
	private val altinnClient: AltinnClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getRollerForPerson(norskIdent: String): List<RollerIOrganisasjon> {
		val person = personRepository.get(norskIdent)

		return when {
			person == null -> {
				val roller = getAndSaveRollerFromAltinn(norskIdent)
				map(roller)
			}

			person.lastSynchronized.isBefore(ZonedDateTime.now().minusHours(1)) -> {
				updateRollerFromAltinn(person.id, norskIdent)
				map(getGyldigeRoller(norskIdent))
			}

			else -> {
				val roller = getGyldigeRoller(norskIdent)

				if (roller.isEmpty()) {
					updateRollerFromAltinn(person.id, norskIdent)
					map(getGyldigeRoller(norskIdent))
				} else {
					map(roller)
				}
			}
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

		val rolleMap: Map<RolleType, List<String>> = try {
			altinnClient.hentRoller(norskIdent, RolleType.entries).filterValues { it.isNotEmpty() }
		} catch (e: Exception) {
			log.warn("Klarte ikke hente roller for ny bruker", e)
			return emptyList()
		}

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

		val rolleMap: Map<RolleType, List<String>> = try {
			altinnClient.hentRoller(norskIdent, RolleType.entries)
		} catch (e: Exception) {
			log.warn("Klarte ikke oppdatere roller for bruker $id, bruker lagrede roller om eksisterer", e)
			return
		}

		rolleMap.forEach { (rolle, organisasjonerMedRolle) ->
			val oldRoller = allOldRoller.filter { it.rolleType == rolle }

			oldRoller.forEach { oldRolle ->
				if (!organisasjonerMedRolle.contains(oldRolle.organisasjonsnummer)) {
					log.debug("User {} lost {} on {}", id, rolle, oldRolle.organisasjonsnummer)
					rolleRepository.invalidateRolle(oldRolle.id)
				}
			}

			organisasjonerMedRolle.forEach { orgRolle ->
				if (oldRoller.none { it.organisasjonsnummer == orgRolle && it.erGyldig() }) {
					log.debug("User {} got {} on {}", id, rolle, orgRolle)
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

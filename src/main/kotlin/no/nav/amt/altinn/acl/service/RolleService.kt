package no.nav.amt.altinn.acl.service

import no.nav.amt.altinn.acl.client.altinn.Altinn3Client
import no.nav.amt.altinn.acl.domain.RolleType
import no.nav.amt.altinn.acl.domain.RollerIOrganisasjon
import no.nav.amt.altinn.acl.repository.PersonRepository
import no.nav.amt.altinn.acl.repository.RolleRepository
import no.nav.amt.altinn.acl.repository.dbo.RolleDbo
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime

@Service
class RolleService(
    private val personRepository: PersonRepository,
    private val rolleRepository: RolleRepository,
    private val altinnClient: Altinn3Client,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getRollerForPerson(norskIdent: String): List<RollerIOrganisasjon> {
        val person = personRepository.get(norskIdent)

        return when {
            person == null -> {
                val roller = getAndSaveRollerFromAltinn(norskIdent)
                roller.mapToRollerIOrganisasjon()
            }

            person.lastSynchronized.isBefore(ZonedDateTime.now().minusHours(1)) -> {
                updateRollerFromAltinn(person.id, norskIdent)
                rolleRepository.hentGyldigeRollerForPerson(norskIdent).mapToRollerIOrganisasjon()
            }

            else -> {
                val roller = rolleRepository.hentGyldigeRollerForPerson(norskIdent)

                if (roller.isEmpty()) {
                    updateRollerFromAltinn(
                        id = person.id,
                        norskIdent = norskIdent,
                    )
                    rolleRepository.hentGyldigeRollerForPerson(norskIdent).mapToRollerIOrganisasjon()
                } else {
                    roller.mapToRollerIOrganisasjon()
                }
            }
        }
    }

    fun synchronizeUsers(
        max: Int = 25,
        synchronizedBefore: OffsetDateTime = OffsetDateTime.now().minusWeeks(1),
    ) {
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
            log.warn(
                "Klarte ikke hente roller for ny bruker, exceptionType={}, statusCode={}, errorCode={}, traceId={}",
                e.javaClass.name,
                e.safeStatusCode(),
                e.safeErrorCode(),
                MDC.get("trace_id"),
            )
            // Feilen fanges her, så API-et svarer 200 med tom rolleliste; personen lagres ikke.
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

        return rolleRepository.hentGyldigeRollerForPerson(norskIdent)
    }

    private fun updateRollerFromAltinn(
        id: Long,
        norskIdent: String,
    ) {
        val start = Instant.now()
        val allOldRoller = rolleRepository.hentGyldigeRollerForPerson(norskIdent)

        val rolleMap: Map<RolleType, List<String>> = try {
            altinnClient.hentRoller(norskIdent, RolleType.entries)
        } catch (e: Exception) {
            log.warn(
                "Klarte ikke oppdatere roller for brukerId={}, bruker lagrede roller om eksisterer, " +
                    "exceptionType={}, statusCode={}, errorCode={}, traceId={}",
                id,
                e.javaClass.name,
                e.safeStatusCode(),
                e.safeErrorCode(),
                MDC.get("trace_id"),
            )
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
}

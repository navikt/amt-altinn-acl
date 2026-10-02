package no.nav.amt.altinn.acl.service

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.verify
import no.nav.amt.altinn.acl.domain.RolleType
import no.nav.amt.altinn.acl.domain.RolleType.KOORDINATOR
import no.nav.amt.altinn.acl.domain.RolleType.VEILEDER
import no.nav.amt.altinn.acl.domain.RollerIOrganisasjon
import no.nav.amt.altinn.acl.repository.PersonRepository
import no.nav.amt.altinn.acl.repository.RolleRepository
import no.nav.amt.altinn.acl.testutil.IntegrationTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class RolleServiceTest(
    private val rolleService: RolleService,
    private val personRepository: PersonRepository,
    private val rolleRepository: RolleRepository,
    private val jdbcTemplate: JdbcTemplate,
) : IntegrationTest() {
    @Nested
    inner class HentRollerForPerson {
        @Test
        fun `oppretter person og henter roller fra Altinn når personen ikke finnes`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            mockAltinnRoller(norskIdent, listOf(KOORDINATOR, VEILEDER), listOf(organisasjonsnummer))

            // Act
            val roller = rolleService.getRollerForPerson(norskIdent)

            // Assert
            verify(exactly = 1) { altinnClient.hentRoller(norskIdent, RolleType.entries) }
            roller.size shouldBe 1
            hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe true
            hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true

            val databasePerson = personRepository.get(norskIdent)
            databasePerson.shouldNotBeNull()
            databasePerson.lastSynchronized.days() shouldBe ZonedDateTime.now().days()

            hasRolleInDatabase(norskIdent, organisasjonsnummer, VEILEDER) shouldBe true
            hasRolleInDatabase(norskIdent, organisasjonsnummer, KOORDINATOR) shouldBe true
        }

        @Test
        fun `lagrer ikke personen når Altinn ikke returnerer roller`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            mockAltinnRoller(norskIdent, listOf(VEILEDER), emptyList())

            // Act
            val roller = rolleService.getRollerForPerson(norskIdent)

            // Assert
            verify(exactly = 1) { altinnClient.hentRoller(norskIdent, RolleType.entries) }
            roller.size shouldBe 0
            personRepository.get(norskIdent) shouldBe null
        }

        @Test
        fun `returnerer lagrede roller uten å kontakte Altinn ved fersk synkronisering`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            val personDbo = personRepository.createAndSetSynchronized(norskIdent)
            rolleRepository.createRolle(
                personId = personDbo.id,
                organisasjonsnummer = organisasjonsnummer,
                rolleType = KOORDINATOR,
            )

            // Act
            rolleService.getRollerForPerson(norskIdent)

            // Assert
            verify(exactly = 0) { altinnClient.hentRoller(any(), any()) }
        }

        @Test
        fun `henter roller fra Altinn når personen ikke har lagrede roller`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            personRepository.createAndSetSynchronized(norskIdent)
            mockAltinnRoller(norskIdent, listOf(KOORDINATOR, VEILEDER), listOf(organisasjonsnummer))

            // Act
            val roller = rolleService.getRollerForPerson(norskIdent)

            // Assert
            hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe true
            hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true
            verify(exactly = 1) { altinnClient.hentRoller(norskIdent, RolleType.entries) }
        }

        @Test
        fun `ugyldiggjør roller som er fjernet i Altinn`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            val personDbo = opprettUsynkronisertPerson(norskIdent)
            rolleRepository.createRolle(personDbo.id, organisasjonsnummer, KOORDINATOR)
            val veilederRolle = rolleRepository.createRolle(personDbo.id, organisasjonsnummer, VEILEDER)
            mockAltinnRoller(norskIdent, listOf(KOORDINATOR), listOf(organisasjonsnummer))

            // Act
            val roller = rolleService.getRollerForPerson(norskIdent)

            // Assert
            hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe false
            hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true
            isRolleGyldig(veilederRolle.id) shouldBe false
        }

        @Test
        fun `legger til roller som er gitt i Altinn`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            val personDbo = opprettUsynkronisertPerson(norskIdent)
            rolleRepository.createRolle(personDbo.id, organisasjonsnummer, VEILEDER)
            mockAltinnRoller(norskIdent, listOf(KOORDINATOR, VEILEDER), listOf(organisasjonsnummer))

            // Act
            val roller = rolleService.getRollerForPerson(norskIdent)

            // Assert
            hasRolle(roller, organisasjonsnummer, VEILEDER) shouldBe true
            hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true
        }

        @Test
        fun `oppretter ny historikkrad når tilgangen gis på nytt`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            val personDbo = opprettUsynkronisertPerson(norskIdent)
            rolleRepository.createRolle(personDbo.id, organisasjonsnummer, KOORDINATOR)
            mockAltinnRoller(norskIdent, listOf(KOORDINATOR, VEILEDER), emptyList())

            // Act
            val rollerUtenTilgang = rolleService.getRollerForPerson(norskIdent)

            // Assert
            rollerUtenTilgang.isEmpty() shouldBe true

            // Arrange
            mockAltinnRoller(norskIdent, listOf(KOORDINATOR), listOf(organisasjonsnummer))

            // Act
            val rollerEtterGjenoppretting = rolleService.getRollerForPerson(norskIdent)

            // Assert
            hasRolle(rollerEtterGjenoppretting, organisasjonsnummer, KOORDINATOR) shouldBe true
            val databaseRoller = jdbcTemplate.query(
                """
                SELECT valid_to IS NULL
                FROM rolle
                WHERE person_id = ? AND rolle = ?
                ORDER BY id
                """.trimIndent(),
                { rs, _ -> rs.getBoolean(1) },
                personDbo.id,
                KOORDINATOR.toString(),
            )
            databaseRoller shouldBe listOf(false, true)
        }

        @Test
        fun `beholder lagrede roller når Altinn ikke er tilgjengelig`() {
            // Arrange
            val norskIdent = UUID.randomUUID().toString()
            val organisasjonsnummer = UUID.randomUUID().toString()
            val personDbo = opprettUsynkronisertPerson(norskIdent)
            rolleRepository.createRolle(personDbo.id, organisasjonsnummer, KOORDINATOR)
            every { altinnClient.hentRoller(norskIdent, RolleType.entries) } throws
                RuntimeException("Klarte ikke å hente organisasjoner code=500")

            // Act
            val roller = rolleService.getRollerForPerson(norskIdent)
            val updatedPerson = personRepository.get(norskIdent)

            // Assert
            verify(exactly = 1) { altinnClient.hentRoller(norskIdent, RolleType.entries) }
            hasRolle(roller, organisasjonsnummer, KOORDINATOR) shouldBe true
            val synchronizedPerson = updatedPerson.shouldNotBeNull()
            synchronizedPerson.lastSynchronized.days() shouldNotBe ZonedDateTime.now().days()
        }
    }

    private fun hasRolleInDatabase(
        norskIdent: String,
        organisasjonsnummerNumber: String,
        rolle: RolleType,
    ): Boolean = rolleRepository
        .hentGyldigeRollerForPerson(norskIdent)
        .any { it.organisasjonsnummer == organisasjonsnummerNumber && it.rolleType == rolle }

    private fun opprettUsynkronisertPerson(norskIdent: String) = personRepository.createAndSetSynchronized(
        norskIdent,
        Instant.EPOCH.atZone(ZoneOffset.UTC),
    )

    private fun isRolleGyldig(rolleId: Long): Boolean = jdbcTemplate
        .query(
            "SELECT valid_to IS NULL FROM rolle WHERE id = ?",
            { rs, _ -> rs.getBoolean(1) },
            rolleId,
        ).single()

    private fun hasRolle(
        list: List<RollerIOrganisasjon>,
        organisasjonsnummerNumber: String,
        rolle: RolleType,
    ): Boolean = list
        .find { it.organisasjonsnummer == organisasjonsnummerNumber }
        ?.roller
        ?.find { it.rolleType == rolle } != null

    private fun ZonedDateTime.days(): ZonedDateTime = this.truncatedTo(ChronoUnit.DAYS)
}

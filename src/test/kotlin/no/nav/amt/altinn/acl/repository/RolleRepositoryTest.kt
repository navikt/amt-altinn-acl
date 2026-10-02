package no.nav.amt.altinn.acl.repository

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.amt.altinn.acl.domain.RolleType
import no.nav.amt.altinn.acl.repository.dbo.PersonDbo
import no.nav.amt.altinn.acl.repository.dbo.RolleDbo
import no.nav.amt.altinn.acl.testutil.RepositoryTestBase
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@SpringBootTest(classes = [RolleRepository::class, PersonRepository::class])
class RolleRepositoryTest(
    private val personRepository: PersonRepository,
    private val rolleRepository: RolleRepository,
    private val jdbcTemplate: JdbcTemplate,
) : RepositoryTestBase() {
    @Nested
    inner class OpprettRolle {
        @Test
        fun `oppretter rolle med riktige verdier`() {
            // Arrange
            val person = opprettPerson()
            val organisasjonsnummer = nyttOrganisasjonsnummer()

            // Act
            val rolle = rolleRepository.createRolle(person.id, organisasjonsnummer, RolleType.VEILEDER)

            // Assert
            assertSoftly(rolle) {
                id shouldBeGreaterThan 0L
                personId shouldBe person.id
                this.organisasjonsnummer shouldBe organisasjonsnummer
                rolleType shouldBe RolleType.VEILEDER
                validTo shouldBe null
                erGyldig() shouldBe true
            }
        }
    }

    @Nested
    inner class UgyldiggjørRolle {
        @Test
        fun `setter tidspunkt for ugyldiggjøring og beholder historikken`() {
            // Arrange
            val person = opprettPerson()
            val rolle = opprettRolle(person)

            // Act
            rolleRepository.invalidateRolle(rolle.id)

            // Assert
            val isValid = jdbcTemplate
                .query(
                    "SELECT valid_to IS NULL FROM rolle WHERE id = ?",
                    { rs, _ -> rs.getBoolean(1) },
                    rolle.id,
                ).single()

            isValid shouldBe false
        }
    }

    @Nested
    inner class HentGyldigeRollerForPerson {
        @Test
        fun `henter bare gyldige roller for personen`() {
            // Arrange
            val person = opprettPerson()
            val gyldigRolle = opprettRolle(person, RolleType.VEILEDER)
            val ugyldigRolle = opprettRolle(person, RolleType.KOORDINATOR)
            rolleRepository.invalidateRolle(ugyldigRolle.id)
            opprettRolle(opprettPerson(), RolleType.KOORDINATOR)

            // Act
            val roller = rolleRepository.hentGyldigeRollerForPerson(person.norskIdent)

            // Assert
            roller.map { it.id } shouldBe listOf(gyldigRolle.id)
            roller.all { it.erGyldig() } shouldBe true
        }
    }

    private fun opprettPerson(): PersonDbo = personRepository.createAndSetSynchronized("ident-${UUID.randomUUID()}")

    private fun opprettRolle(
        person: PersonDbo,
        rolleType: RolleType = RolleType.VEILEDER,
    ): RolleDbo = rolleRepository.createRolle(
        personId = person.id,
        organisasjonsnummer = nyttOrganisasjonsnummer(),
        rolleType = rolleType,
    )

    private fun nyttOrganisasjonsnummer(): String = UUID.randomUUID().toString()
}

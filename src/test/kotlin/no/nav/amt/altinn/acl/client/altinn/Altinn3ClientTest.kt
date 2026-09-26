package no.nav.amt.altinn.acl.client.altinn

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.altinn.acl.domain.RolleType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.RestClientResponseException
import java.nio.charset.StandardCharsets

class Altinn3ClientTest {
    private val altinnApi = mockk<AltinnApi>()
    private val altinnClient = Altinn3Client(altinnApi)

    @Test
    fun `hentRoller - flere tilganger - parser response riktig`() {
        val resourceIds = listOf(RolleType.KOORDINATOR.resourceId, RolleType.VEILEDER.resourceId, "resource3")

        val parties = listOf(
            AuthorizedParty(
                organizationNumber = "123456789",
                authorizedResources = setOf(resourceIds[0], resourceIds[1]),
                subunits = emptyList(),
            ),
            AuthorizedParty(
                organizationNumber = "987654321",
                authorizedResources = setOf(resourceIds[2]),
                subunits =
                    listOf(
                        AuthorizedParty(
                            organizationNumber = "111222333",
                            authorizedResources = setOf(resourceIds[1]),
                            subunits = emptyList(),
                        ),
                    ),
            ),
            AuthorizedParty(
                organizationNumber = "456789012",
                authorizedResources = setOf(resourceIds[0], resourceIds[2]),
                subunits =
                    listOf(
                        AuthorizedParty(
                            organizationNumber = "333444555",
                            authorizedResources = setOf(resourceIds[1]),
                            subunits =
                                listOf(
                                    AuthorizedParty(
                                        organizationNumber = "666777888",
                                        authorizedResources = setOf(resourceIds[0]),
                                        subunits = emptyList(),
                                    ),
                                ),
                        ),
                    ),
            ),
        )

        every { altinnApi.hentAuthorizedParties(any()) } returns parties

        val organisasjoner = altinnClient.hentRoller("123456", RolleType.entries)

        val koordinatorRoller = organisasjoner[RolleType.KOORDINATOR]
        koordinatorRoller.shouldNotBeNull()
        koordinatorRoller shouldHaveSize 3

        val veilederRoller = organisasjoner[RolleType.VEILEDER]
        veilederRoller.shouldNotBeNull()
        veilederRoller shouldHaveSize 3
    }

    @Test
    fun `hentRoller - AltinnApi kaster feil - kaster sanitert feil uten response body`() {
        val norskIdent = "12345678901"
        every { altinnApi.hentAuthorizedParties(any()) } throws
            RestClientResponseException(
                "feil",
                HttpStatusCode.valueOf(500),
                "Internal Server Error",
                null,
                """{"norskIdent":"$norskIdent"}""".toByteArray(),
                StandardCharsets.UTF_8,
            )

        val exception = assertThrows<RuntimeException> {
            altinnClient.hentRoller(norskIdent, RolleType.entries)
        }

        exception.message shouldBe "Klarte ikke å hente organisasjoner code=500"
        exception.cause shouldBe null
    }
}

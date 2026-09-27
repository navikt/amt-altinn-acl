package no.nav.amt.altinn.acl.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.amt.altinn.acl.service.RolleService
import no.nav.amt.altinn.acl.testutil.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class InternalControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val rolleService: RolleService,
) : IntegrationTest() {
    @BeforeEach
    fun setup() {
        justRun { rolleService.synchronizeUsers(25, any()) }
    }

    /**
     * MockMvc bruker remoteAddr = "127.0.0.1" som standard, så requests regnes som interne.
     * Denne overstyrer adressen for å simulere et kall utenfra clusteret.
     */
    private fun MockHttpServletRequestDsl.fraEksternAdresse() {
        with { request ->
            request.remoteAddr = "10.0.0.1"
            request
        }
    }

    @Test
    fun `synkroniserAltinnRettigheter - intern adresse - returnerer 200 og starter synkronisering`() {
        mockMvc
            .get(PATH)
            .andExpect { status { isOk() } }

        verify(exactly = 1) { rolleService.synchronizeUsers(25, any()) }
    }

    @Test
    fun `synkroniserAltinnRettigheter - intern feil gir 500 uten feildetaljer`() {
        every { rolleService.synchronizeUsers(25, any()) } throws IllegalStateException("Intern feildetalj")

        mockMvc
            .get(PATH)
            .andExpect {
                status { isInternalServerError() }
                content {
                    json(
                        """{"status":500,"title":"500 INTERNAL_SERVER_ERROR","detail":"En uventet feil oppstod"}""",
                        JsonCompareMode.STRICT,
                    )
                }
            }

        verify(exactly = 1) { rolleService.synchronizeUsers(25, any()) }
    }

    @Test
    fun `synkroniserAltinnRettigheter - ekstern adresse - returnerer 401 og synkroniserer ikke`() {
        mockMvc
            .get(PATH) { fraEksternAdresse() }
            .andExpect { status { isUnauthorized() } }

        verify(exactly = 0) { rolleService.synchronizeUsers(25, any()) }
    }

    companion object {
        private const val PATH = "/internal/altinn/synkroniser"
    }
}

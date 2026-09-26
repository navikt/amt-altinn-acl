package no.nav.amt.altinn.acl.client.altinn

import io.kotest.matchers.shouldBe
import no.nav.amt.altinn.acl.client.RestClientTestBase
import no.nav.amt.altinn.acl.domain.RolleType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 * Tester HTTP-kontrakten til AltinnApi mot en ekte deklarativ RestClient-proxy (via @ImportHttpServices),
 * i stedet for å mocke AltinnApi-grensesnittet direkte. Dette verifiserer at:
 * - riktig URL/metode/request-body faktisk sendes,
 * - Authorization-headeren inneholder et ekte Bearer-token satt av OAuth2-integrasjonen
 *   (jf. den historiske "******"-buggen der en literal streng ble sendt i stedet for et token).
 */
@RestClientTest(Altinn3Client::class)
class AltinnApiTest(
    private val sut: Altinn3Client,
) : RestClientTestBase("altinn3") {
    @Test
    fun `hentRoller - sender riktig request med ekte Bearer-token`() {
        server
            .expect(requestTo("http://altinn3/accessmanagement/api/v1/resourceowner/authorizedparties"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer altinn3-token"))
            .andExpect(content().json("""{"value":"12345678910","type":"urn:altinn:person:identifier-no"}"""))
            .andRespond(
                withSuccess(
                    """
                    [
                      {
                        "organizationNumber": "123456789",
                        "authorizedResources": ["${RolleType.KOORDINATOR.resourceId}"],
                        "subunits": []
                      }
                    ]
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val roller = sut.hentRoller("12345678910", listOf(RolleType.KOORDINATOR))

        roller[RolleType.KOORDINATOR] shouldBe listOf("123456789")
    }

    @Test
    fun `hentRoller - feilrespons kaster RuntimeException`() {
        server
            .expect(requestTo("http://altinn3/accessmanagement/api/v1/resourceowner/authorizedparties"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows<RuntimeException> {
            sut.hentRoller("12345678910", listOf(RolleType.KOORDINATOR))
        }
    }
}

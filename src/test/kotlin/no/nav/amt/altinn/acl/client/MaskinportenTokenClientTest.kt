package no.nav.amt.altinn.acl.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.util.LinkedMultiValueMap

@RestClientTest(MaskinportenTokenClient::class)
@TestPropertySource(
    properties = [
        "maskinporten.token-endpoint=http://localhost/token",
        "maskinporten.scopes=scope1 scope2",
        "altinn3.url=http://altinn3",
    ],
)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MaskinportenTokenClientTest(
    private val sut: MaskinportenTokenClient,
    private val server: MockRestServiceServer,
) {
    @AfterEach
    fun verifyServer() = server.verify()

    @Test
    fun `henter access token fra Nais med riktige skjemadata`() {
        val expectedForm = LinkedMultiValueMap<String, String>().apply {
            add("identity_provider", "maskinporten")
            add("target", "scope1 scope2")
            add("resource", "http://altinn3")
        }
        server
            .expect(requestTo("http://localhost/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formData(expectedForm))
            .andRespond(withSuccess("""{"access_token":"maskinporten-access-token"}""", MediaType.APPLICATION_JSON))

        sut.getAccessToken() shouldBe "maskinporten-access-token"
    }

    @Test
    fun `feil fra Nais sitt token-endepunkt gir sanitert feil med statuskode`() {
        server
            .expect(requestTo("http://localhost/token"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("""{"error":"token request failed"}"""))

        val exception = shouldThrow<MaskinportenTokenClient.MaskinportenTokenException> {
            sut.getAccessToken()
        }

        exception.message shouldBe "Klarte ikke å hente Maskinporten-token code=500"
        exception.cause shouldBe null
    }
}

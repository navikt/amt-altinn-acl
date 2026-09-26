package no.nav.amt.altinn.acl.client.maskinporten

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.altinn.acl.testutil.Constants.TEST_JWK
import no.nav.amt.altinn.acl.testutil.mockclients.MockMaskinportenHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.web.client.RestClient
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class MaskinportenJwtBearerProviderTest {
    private lateinit var mockServer: MockMaskinportenHttpClient

    private val assertionBuilder = MaskinportenJwtAssertionBuilder(
        clientId = "client-id",
        issuer = "https://issuer.example",
        resource = "https://platform.tt02.altinn.no",
        scopes = listOf("scope1", "scope2"),
        privateJwk = TEST_JWK,
    )

    private val provider = maskinportenJwtBearerProvider(
        assertionBuilder = assertionBuilder,
        restClientBuilder = RestClient.builder(),
    )
    private val principal = TestingAuthenticationToken("system", "n/a")

    @BeforeEach
    fun start() {
        mockServer = MockMaskinportenHttpClient()
        mockServer.start()
    }

    @AfterEach
    fun stop() = mockServer.stop()

    private fun registration(tokenUri: String = "${mockServer.serverUrl()}/token"): ClientRegistration = ClientRegistration
        .withRegistrationId("altinn3")
        .clientId("client-id")
        .tokenUri(tokenUri)
        .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .scope("scope1", "scope2")
        .build()

    @Test
    fun `authorize - ingen eksisterende token - henter nytt token fra maskinporten`() {
        mockServer.enqueueTokenResponse()

        val context = OAuth2AuthorizationContext
            .withClientRegistration(registration())
            .principal(principal)
            .build()

        val authorizedClient = provider.authorize(context)

        authorizedClient.shouldNotBe(null)
        authorizedClient!!.accessToken.tokenValue.shouldNotBe(null)

        val request = mockServer.takeRequest()
        val formData = parseFormData(request.body)

        request.path shouldBe "/token"
        request.method shouldBe "POST"
        formData["grant_type"] shouldBe "urn:ietf:params:oauth:grant-type:jwt-bearer"
        formData["client_id"] shouldBe "client-id"
        formData["scope"] shouldBe "scope1 scope2"
        formData["assertion"].shouldNotBe(null)
    }

    @Test
    fun `authorize - token fortsatt gyldig - gjenbruker eksisterende, kaller ikke maskinporten`() {
        val gyldigToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "eksisterende-token",
            Instant.now(),
            Instant.now().plusSeconds(300),
        )
        val existing = OAuth2AuthorizedClient(registration(), "system", gyldigToken)

        val context = OAuth2AuthorizationContext
            .withAuthorizedClient(existing)
            .principal(principal)
            .build()

        // Ingen token er lagt i køen på mockServer - hvis provideren feilaktig kaller
        // maskinporten likevel, vil mock-serveren feile fordi ingen respons er enqueued.
        val result = provider.authorize(context)

        result shouldBe null
    }

    @Test
    fun `authorize - token nær utløp - henter nytt token med ti sekunders klokkeslakk`() {
        mockServer.enqueueTokenResponse()

        val utloptToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "utlopt-token",
            Instant.now().minusSeconds(600),
            Instant.now().plusSeconds(5),
        )
        val existing = OAuth2AuthorizedClient(registration(), "system", utloptToken)

        val context = OAuth2AuthorizationContext
            .withAuthorizedClient(existing)
            .principal(principal)
            .build()

        val result = provider.authorize(context)

        result.shouldNotBe(null)
        result!!.accessToken.tokenValue shouldNotBe "utlopt-token"
    }

    @Test
    fun `authorize - tokenrespons mangler expires in - bruker standard levetid`() {
        mockServer.enqueue(
            headers = mapOf("Content-Type" to "application/json"),
            body = """{ "token_type": "Bearer", "access_token": "token-without-expiration" }""",
        )

        val context = OAuth2AuthorizationContext
            .withClientRegistration(registration())
            .principal(principal)
            .build()

        val result = provider.authorize(context)

        result.shouldNotBe(null)
        val secondsUntilExpiry = result!!.accessToken.expiresAt!!.epochSecond - Instant.now().epochSecond
        (secondsUntilExpiry in 119L..120L) shouldBe true
    }

    @Test
    fun `authorize - maskinporten svarer med feil - kaster sanitert exception uten response body`() {
        mockServer.enqueue(
            responseCode = 400,
            body = """{ "error": "invalid_grant", "error_description": "assertion 12345678901 expired" }""",
        )

        val context = OAuth2AuthorizationContext
            .withClientRegistration(registration())
            .principal(principal)
            .build()

        val exception = assertThrows<RuntimeException> {
            provider.authorize(context)
        }

        val exceptionMessages = generateSequence<Throwable>(exception) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")

        exceptionMessages.contains("code=400") shouldBe true
        exceptionMessages.contains("assertion 12345678901 expired") shouldBe false
    }

    private fun parseFormData(formData: String): Map<String, String> = formData
        .split("&")
        .associate { value ->
            val (key, encodedValue) = value.split("=", limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8) to
                URLDecoder.decode(encodedValue, StandardCharsets.UTF_8)
        }
}

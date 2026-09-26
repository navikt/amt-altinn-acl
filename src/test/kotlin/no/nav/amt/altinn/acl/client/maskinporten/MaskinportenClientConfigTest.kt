package no.nav.amt.altinn.acl.client.maskinporten

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager

class MaskinportenClientConfigTest {
    @Test
    fun `authorized client manager bruker samme principal for alle kall`() {
        val capturedRequests = mutableListOf<OAuth2AuthorizeRequest>()
        val delegate = mockk<OAuth2AuthorizedClientManager>()
        val requestSlot = slot<OAuth2AuthorizeRequest>()
        every { delegate.authorize(capture(requestSlot)) } answers {
            capturedRequests.add(requestSlot.captured)
            null
        }
        val manager = fixedPrincipalManager(delegate)

        manager.authorize(authorizeRequest("caller-one"))
        manager.authorize(authorizeRequest("caller-two"))

        verify(exactly = 2) { delegate.authorize(any()) }
        capturedRequests.map { it.principal.name } shouldBe
            listOf("maskinporten-system", "maskinporten-system")
    }

    private fun authorizeRequest(principalName: String): OAuth2AuthorizeRequest = OAuth2AuthorizeRequest
        .withClientRegistrationId("altinn3")
        .principal(TestingAuthenticationToken(principalName, "n/a"))
        .build()
}

package no.nav.amt.altinn.acl.client

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import java.time.Instant

/**
 * Testkonfigurasjon for deklarative HTTP-klienter (jf. amt-arrangor sitt RestClientTestBase-mønster).
 *
 * Binder hver http-klient-gruppe (definert via @ImportHttpServices) til sin egen MockRestServiceServer,
 * og erstatter den ekte Maskinporten-autoriseringen (MaskinportenAuthorizedClientProvider) med en enkel,
 * deterministisk OAuth2AuthorizedClientManager - vi tester HTTP-kontrakten til klienten, ikke Maskinporten-flyten.
 */
@TestConfiguration(proxyBeanMethods = false)
class ClientTestConfig {
    private val mocks = mutableMapOf<String, MockRestServiceServer>()

    @Bean
    fun mockServerConfigurer(environment: Environment) = RestClientHttpServiceGroupConfigurer { groups ->
        groups.forEachClient { group, builder ->
            val baseUrl = environment.getRequiredProperty("spring.http.serviceclient.${group.name()}.base-url")
            builder.baseUrl(baseUrl)
            mocks[group.name()] = MockRestServiceServer.bindTo(builder).build()
        }
    }

    @Bean
    @Primary
    fun authorizedClientManager(): OAuth2AuthorizedClientManager = OAuth2AuthorizedClientManager { authorizeRequest ->
        val registrationId = authorizeRequest.clientRegistrationId

        val registration = ClientRegistration
            .withRegistrationId(registrationId)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .clientId(registrationId)
            .clientSecret("test-secret")
            .tokenUri("http://localhost:9999/token")
            .build()

        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "$registrationId-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
        )

        OAuth2AuthorizedClient(registration, registrationId, accessToken)
    }

    @Bean
    fun oauth2Configurer(manager: OAuth2AuthorizedClientManager) = OAuth2RestClientHttpServiceGroupConfigurer.from(manager)

    fun getMock(group: String): MockRestServiceServer = mocks[group] ?: error("No mock for group '$group'")
}

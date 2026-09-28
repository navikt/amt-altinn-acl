package no.nav.amt.altinn.acl.config

import no.nav.amt.altinn.acl.client.MaskinportenTokenClient
import no.nav.amt.altinn.acl.client.altinn.ALTINN3_CLIENT_ID
import no.nav.amt.altinn.acl.client.altinn.AltinnApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.ImportHttpServices

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = ALTINN3_CLIENT_ID, types = [AltinnApi::class])
class ClientConfig {
    @Bean
    fun httpServiceGroupConfigurer(maskinportenTokenClient: MaskinportenTokenClient) = RestClientHttpServiceGroupConfigurer { groups ->
        groups.forEachClient { _, builder ->
            // legges det til flere klienter, må det sjekkes på navn her
            builder.requestInterceptor { request, body, execution ->
                request.headers.setBearerAuth(maskinportenTokenClient.getAccessToken())
                execution.execute(request, body)
            }
        }
    }
}

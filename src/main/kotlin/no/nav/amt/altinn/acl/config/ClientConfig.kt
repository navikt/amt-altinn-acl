package no.nav.amt.altinn.acl.config

import no.nav.amt.altinn.acl.client.MaskinportenTokenClient
import no.nav.amt.altinn.acl.client.altinn.ALTINN3_CLIENT_ID
import no.nav.amt.altinn.acl.client.altinn.AltinnApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.ImportHttpServices

@Configuration(proxyBeanMethods = false)
// Det er én HTTP-klient per gruppe, så vi bruker klient-ID som gruppenavn
@ImportHttpServices(group = ALTINN3_CLIENT_ID, types = [AltinnApi::class])
class ClientConfig {
    @Bean
    fun httpServiceGroupConfigurer(maskinportenTokenClient: MaskinportenTokenClient) = RestClientHttpServiceGroupConfigurer { groups ->
        groups.forEachClient { group, builder ->
            builder.defaultHeaders { headers ->
                headers.accept = listOf(MediaType.APPLICATION_JSON)
            }
            if (group.name() == ALTINN3_CLIENT_ID) {
                builder.requestInterceptor { request, body, execution ->
                    request.headers.setBearerAuth(maskinportenTokenClient.getAccessToken())
                    execution.execute(request, body)
                }
            }
        }
    }
}

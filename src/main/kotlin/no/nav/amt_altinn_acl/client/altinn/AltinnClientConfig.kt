package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.client.maskinporten.MaskinportenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AltinnClientConfig {

	@Value("\${altinn3.url}")
	lateinit var altinn3Url: String

	@Bean
	fun altinnClient(maskinportenClient: MaskinportenClient): AltinnClient {
		return Altinn3ClientImpl(
			baseUrl = altinn3Url,
			maskinportenTokenProvider = maskinportenClient::hentAltinn3Token
		)

	}

}

package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.client.maskinporten.MaskinportenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AltinnClientConfig {

	@Value("\${altinn.url}")
	lateinit var altinnUrl: String

	@Value("\${altinn3.url}")
	lateinit var altinn3Url: String

	@Value("\${altinn.api-key}")
	lateinit var altinnApiKey: String

	@Bean
	fun altinnClient(maskinportenClient: MaskinportenClient): AltinnClient {
		return AltinnClientImpl(
			baseUrl = altinnUrl,
			altinnApiKey = altinnApiKey,
			maskinportenTokenProvider = maskinportenClient::hentAltinnToken
		)
	}

}

package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.client.maskinporten.MaskinportenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class AltinnClientConfig {

	@Bean
	fun altinnClient(
		maskinportenClient: MaskinportenClient,
		@Value($$"${altinn3.url}")  altinn3Url: String
	): AltinnClient = Altinn3ClientImpl(
		baseUrl = altinn3Url,
		maskinportenTokenProvider = maskinportenClient::hentAltinn3Token
	)
}

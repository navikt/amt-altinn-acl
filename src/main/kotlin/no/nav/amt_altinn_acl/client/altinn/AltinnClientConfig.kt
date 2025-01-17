package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.client.maskinporten.MaskinportenClient
import no.nav.amt_altinn_acl.client.unleash.UnleashClient
import no.nav.amt_altinn_acl.domain.RolleType
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
	fun altinnClient(maskinportenClient: MaskinportenClient, unleashClient: UnleashClient): AltinnClient {
		val altinn2Client = AltinnClientImpl(
			baseUrl = altinnUrl,
			altinnApiKey = altinnApiKey,
			maskinportenTokenProvider = maskinportenClient::hentAltinnToken
		)
		val altinn3Client = Altinn3ClientImpl(
			baseUrl = altinn3Url,
			altinnApiKey = "altinn3-api-key", // TODO: api-key
			maskinportenTokenProvider = maskinportenClient::hentAltinn3Token
		)

		return AltinnClientWrapper(altinn2Client, altinn3Client, unleashClient)
	}

}


class AltinnClientWrapper(
	private val altinn2Client: AltinnClientImpl,
	private val altinn3Client: Altinn3ClientImpl,
	private val unleashClient: UnleashClient,
): AltinnClient {
	override fun hentAlleOrganisasjoner(norskIdent: String, roller: List<RolleType>): Map<RolleType, List<String>> {
		return if (unleashClient.erAltinn3Enabled()) {
			altinn3Client.hentAlleOrganisasjoner(norskIdent, roller)
		} else {
			altinn2Client.hentAlleOrganisasjoner(norskIdent, roller)
		}
	}

}


package no.nav.amt_altinn_acl.client.maskinporten

import no.nav.common.token_client.cache.CaffeineTokenCache

class CachedMaskinportenClient(
	private val maskinportenClient: MaskinportenClient
) : MaskinportenClient {

	private val altinnKey = "altinn"
	private val altinn3Key = "altinn3"

	private val cache = CaffeineTokenCache()

	override fun hentAltinnToken(): String {
		return cache.getFromCacheOrTryProvider(altinnKey, maskinportenClient::hentAltinnToken)
	}

	override fun hentAltinn3Token(): String {
		return cache.getFromCacheOrTryProvider(altinn3Key, maskinportenClient::hentAltinn3Token)
	}

}

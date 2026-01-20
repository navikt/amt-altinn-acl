package no.nav.amt_altinn_acl.client.maskinporten

import no.nav.common.token_client.cache.CaffeineTokenCache

class CachedMaskinportenClient(
	private val maskinportenClient: MaskinportenClient,
) : MaskinportenClient {
	private val cache = CaffeineTokenCache()

	override fun hentAltinn3Token(): String =
		cache.getFromCacheOrTryProvider(
			ALTINN3_KEY,
			maskinportenClient::hentAltinn3Token,
		)

	companion object {
		private const val ALTINN3_KEY = "altinn3"
	}
}

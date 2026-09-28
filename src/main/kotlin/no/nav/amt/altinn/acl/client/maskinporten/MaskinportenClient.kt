package no.nav.amt.altinn.acl.client.maskinporten

fun interface MaskinportenClient {
	fun hentAltinn3Token(): String
}

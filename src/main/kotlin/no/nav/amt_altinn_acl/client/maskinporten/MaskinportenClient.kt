package no.nav.amt_altinn_acl.client.maskinporten

interface MaskinportenClient {

	fun hentAltinnToken(): String

	fun hentAltinn3Token(): String

}

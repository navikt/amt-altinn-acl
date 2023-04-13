package no.nav.amt_altinn_acl.client.altinn

interface AltinnClient {
	fun hentOrganisasjoner(norskIdent: String, serviceCode: String): Result<List<String>>

	fun hentAlleOrganisasjoner(norskIdent: String, serviceCode: String): List<String>
}

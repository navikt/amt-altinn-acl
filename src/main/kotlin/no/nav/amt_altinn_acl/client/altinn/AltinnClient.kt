package no.nav.amt_altinn_acl.client.altinn

interface AltinnClient {
	fun hentOrganisasjoner(norskIdent: String, serviceCode: String): Result<List<String>>

	@Deprecated("Slettes etter ny kode er verifisert")
	fun hentRettigheter(norskIdent: String, organisasjonsnummer: String): List<AltinnRettighet>
}

data class AltinnRettighet(
	val serviceCode: String
)

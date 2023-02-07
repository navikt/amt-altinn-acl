package no.nav.amt_altinn_acl.client.altinn

interface AltinnClient {

	fun hentTilknyttedeOrganisasjoner(norskIdent: String): List<Organisasjon>

	fun hentRettigheter(norskIdent: String, organisasjonsnummer: String): List<AltinnRettighet>

	fun hentOrganisasjoner(norskIdent: String, serviceCode: String?): String
}

data class Organisasjon(
	val organisasjonsnummer: String,
	val type: Type
) {
	enum class Type {
		OVERORDNET_ENHET,
		UNDERENHET
	}
}

data class AltinnRettighet(
	val serviceCode: String
)

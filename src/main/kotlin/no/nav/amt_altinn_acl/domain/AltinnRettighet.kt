package no.nav.amt_altinn_acl.domain

data class AltinnRettighet(
	val organisasjonsnummer: String,
	val serviceCode: String
)

enum class Rettighet {
	VEILEDER, KOORDINATOR
}

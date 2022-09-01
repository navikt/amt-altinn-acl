package no.nav.amt_altinn_acl.domain

data class AltinnRettighet(
	val organisasjonsnummmer: String,
	val rettighetId: String, // Er representert som et heltall i Altinn, men behandles som en String her for mer fleksibilitet
)

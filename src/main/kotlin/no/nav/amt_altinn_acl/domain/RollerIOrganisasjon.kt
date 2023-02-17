package no.nav.amt_altinn_acl.domain

data class RollerIOrganisasjon(
	val organisasjonsnummer: String,
	val roller: List<Rolle>
)

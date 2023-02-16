package no.nav.amt_altinn_acl.domain

data class RightsOnOrganization(
	val organizationNumber: String,
	val rights: List<Right>
)

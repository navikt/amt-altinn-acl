package no.nav.amt_altinn_acl.domain

data class RollerInOrganization(
	val organizationNumber: String,
	val roller: List<Rolle>
)

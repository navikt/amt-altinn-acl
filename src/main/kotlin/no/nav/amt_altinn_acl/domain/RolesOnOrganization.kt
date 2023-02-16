package no.nav.amt_altinn_acl.domain

data class RolesOnOrganization(
	val organizationNumber: String,
	val roles: List<Role>
)

package no.nav.amt_altinn_acl.domain

import java.time.ZonedDateTime

data class Role(
	val id: Long,
	val roleType: RoleType,
	val validFrom: ZonedDateTime,
	val validTo: ZonedDateTime?
)

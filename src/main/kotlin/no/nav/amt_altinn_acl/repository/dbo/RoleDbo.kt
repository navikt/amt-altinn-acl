package no.nav.amt_altinn_acl.repository.dbo

import no.nav.amt_altinn_acl.domain.RoleType
import java.time.ZonedDateTime

data class RoleDbo(
	val id: Long,
	val personId: Long,
	val organizationNumber: String,
	val roleType: RoleType,
	val validFrom: ZonedDateTime,
	val validTo: ZonedDateTime?
)

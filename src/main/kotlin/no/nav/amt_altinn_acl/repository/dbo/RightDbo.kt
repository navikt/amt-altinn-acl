package no.nav.amt_altinn_acl.repository.dbo

import no.nav.amt_altinn_acl.domain.RightType
import java.time.ZonedDateTime

data class RightDbo(
	val id: Long,
	val personId: Long,
	val organizationNumber: String,
	val rightType: RightType,
	val validFrom: ZonedDateTime,
	val validTo: ZonedDateTime?
)

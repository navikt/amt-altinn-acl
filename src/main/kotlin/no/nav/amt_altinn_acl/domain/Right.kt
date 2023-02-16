package no.nav.amt_altinn_acl.domain

import java.time.ZonedDateTime

data class Right(
	val id: Long,
	val rightType: RightType,
	val validFrom: ZonedDateTime,
	val validTo: ZonedDateTime?
)

package no.nav.amt_altinn_acl.repository.dbo

import java.time.ZonedDateTime

data class PersonDbo(
	val id: Long,
	val norskIdent: String,
	val created: ZonedDateTime,
	val lastSynchronized: ZonedDateTime
)

package no.nav.amt_altinn_acl.domain

import java.time.ZonedDateTime

data class Bruker(
	val personligIdent: String,
	val expiresAfter: ZonedDateTime,
	val tilganger: List<AltinnRettighet>,
)

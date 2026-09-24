package no.nav.amt.altinn.acl.client.altinn

import no.nav.amt.altinn.acl.domain.RolleType

fun interface AltinnClient {
	fun hentRoller(
		norskIdent: String,
		roller: List<RolleType>,
	): Map<RolleType, List<String>>
}

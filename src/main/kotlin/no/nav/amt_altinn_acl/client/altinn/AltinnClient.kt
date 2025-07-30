package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.domain.RolleType

fun interface AltinnClient {
	fun hentRoller(norskIdent: String, roller: List<RolleType>): Map<RolleType, List<String>>
}

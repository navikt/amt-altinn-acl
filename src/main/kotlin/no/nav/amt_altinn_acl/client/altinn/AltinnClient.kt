package no.nav.amt_altinn_acl.client.altinn

import no.nav.amt_altinn_acl.domain.RolleType

interface AltinnClient {
	fun hentAlleOrganisasjoner(norskIdent: String, rolle: RolleType): List<String>
}

package no.nav.amt.altinn.acl.service

import no.nav.amt.altinn.acl.domain.Rolle
import no.nav.amt.altinn.acl.domain.RollerIOrganisasjon
import no.nav.amt.altinn.acl.repository.dbo.RolleDbo

fun List<RolleDbo>.mapToRollerIOrganisasjon(): List<RollerIOrganisasjon> {
    val rollerPerOrganisasjon = groupBy { it.organisasjonsnummer }

    return rollerPerOrganisasjon.map { org ->
        RollerIOrganisasjon(
            organisasjonsnummer = org.key,
            roller = org.value.map { rolle ->
                Rolle(
                    id = rolle.id,
                    rolleType = rolle.rolleType,
                    validFrom = rolle.validFrom,
                    validTo = rolle.validTo,
                )
            },
        )
    }
}

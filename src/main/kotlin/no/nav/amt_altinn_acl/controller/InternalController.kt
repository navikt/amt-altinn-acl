package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.client.altinn.AltinnRettighet
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/internal")
class InternalController (
	private val altinnClient: AltinnClient
) {
	@Unprotected
	@GetMapping("/altinn/organisasjoner")
	fun hentOrganisasjoner(
		servlet: HttpServletRequest,
		@RequestParam("fnr") fnr: String,
		@RequestParam("serviceCode") serviceCode: String,
	) : String {
		if (isInternal(servlet)) {
			return altinnClient.hentOrganisasjoner(fnr, serviceCode)
		}
		throw RuntimeException("No access")
	}

	@Unprotected
	@GetMapping("/altinn/rettigheter")
	fun hentRettigheter(
		servlet: HttpServletRequest,
		@RequestParam("fnr") fnr: String,
		@RequestParam("orgNr") orgNr: String,
	) : List<AltinnRettighet> {
		if (isInternal(servlet)) {
			return altinnClient.hentRettigheter(norskIdent = fnr, orgNr)
		}
		throw RuntimeException("No access")
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}

}

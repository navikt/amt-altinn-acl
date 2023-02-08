package no.nav.amt_altinn_acl.controller

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.client.altinn.AltinnRettighet
import no.nav.amt_altinn_acl.utils.SecureLog.secureLog
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
		secureLog.info("Reached /altinn/organisasjoner")
		if (isInternal(servlet)) {
			secureLog.info("Passed internal /altinn/organisasjoner")
			return altinnClient.hentOrganisasjoner(fnr, serviceCode)
		}
		secureLog.error("Attempted external access to /altinn/organisasjoner")
		throw RuntimeException("No access")
	}

	@Unprotected
	@GetMapping("/altinn/rettigheter")
	fun hentRettigheter(
		servlet: HttpServletRequest,
		@RequestParam("fnr") fnr: String,
		@RequestParam("orgNr") orgNr: String,
	) : List<AltinnRettighet> {
		secureLog.info("Reached /altinn/rettigheter")
		if (isInternal(servlet)) {
			secureLog.info("Passed internal /altinn/rettigheter")
			return altinnClient.hentRettigheter(norskIdent = fnr, orgNr)
		}
		secureLog.error("Attempted external access to /altinn/rettigheter")
		throw RuntimeException("No access")
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}

}

package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.client.altinn.AltinnRettighet
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
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
	private val log = LoggerFactory.getLogger(javaClass)

	@Unprotected
	@GetMapping("/altinn/organisasjoner")
	fun hentOrganisasjoner(
		servlet: HttpServletRequest,
		@RequestParam("fnr") fnr: String,
		@RequestParam("serviceCode") serviceCode: String,
	) : String {
		if (isInternal(servlet)) {
			log.info("Reached /altinn/organisasjoner")
			return altinnClient.hentOrganisasjoner(fnr, serviceCode)
		}
		log.error("Attempted external access to /altinn/organisasjoner")
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
			log.info("Reached /altinn/rettigheter")
			return altinnClient.hentRettigheter(norskIdent = fnr, orgNr)
		}
		log.error("Attempted external access to /altinn/rettigheter")
		throw RuntimeException("No access")
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}

}

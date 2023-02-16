package no.nav.amt_altinn_acl.controller

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.amt_altinn_acl.jobs.AltinnUpdater
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class InternalController(
	private val altinnClient: AltinnClient,
	private val altinnUpdater: AltinnUpdater
) {
	@Unprotected
	@GetMapping("/altinn/organisasjoner")
	fun hentOrganisasjoner(
		servlet: HttpServletRequest,
		@RequestParam("fnr") fnr: String,
		@RequestParam("serviceCode") serviceCode: String,
	): List<String> {
		if (isInternal(servlet)) {
			return altinnClient.hentOrganisasjoner(fnr, serviceCode).getOrThrow()
		}
		throw RuntimeException("No access")
	}

	@Unprotected
	@GetMapping("/altinn/synkroniser")
	fun synkroniserAltinnRettigheter(
		servlet: HttpServletRequest,
	) {
		if (isInternal(servlet)) {
			altinnUpdater.update()
		} else {
			throw RuntimeException("No access")
		}
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}

}

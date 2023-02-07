package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.client.altinn.AltinnClient
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.QueryParam

@RestController
@RequestMapping("/internal")
class InternalController (
	private val altinnClient: AltinnClient
) {
	@Unprotected
	@GetMapping("/altinn")
	fun hentOrganisasjoner(
		servlet: HttpServletRequest,
		@QueryParam("fnr") fnr: String,
		@QueryParam("serviceCode") serviceCode: String,
	) : String {
		if (isInternal(servlet)) {
			return altinnClient.hentOrganisasjoner(fnr, serviceCode)
		}
		throw RuntimeException("No access")
	}


	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}

}

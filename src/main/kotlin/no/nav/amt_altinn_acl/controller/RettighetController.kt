package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.service.AuthService
import no.nav.amt_altinn_acl.service.RettigheterService
import no.nav.amt_altinn_acl.utils.Issuer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rettighet")
class RettighetController(
	private val authService: AuthService,
	private val rettigheterService: RettigheterService
) {

	@PostMapping("/hent")
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	fun hentRettigheter(@RequestBody request: HentRettigheter.Request): HentRettigheter.Response {
		authService.verifyRequestIsMachineToMachine()

		val rettigheter = rettigheterService.hentAlleRettigheter(request.norskIdent)
			.filter { request.rettighetIder.contains(it.rettighetId) }
			.map { HentRettigheter.Response.Rettighet(it.rettighetId, it.organisasjonsnummmer) }

		return HentRettigheter.Response(rettigheter)
	}

	object HentRettigheter {
		data class Request(
			val norskIdent: String,
			val rettighetIder: List<String>,
		)

		data class Response(
			val rettigheter: List<Rettighet>
		) {
			data class Rettighet(
				val id: String,
				val organisasjonsnummer: String,
			)
		}
	}

}

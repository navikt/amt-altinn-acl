package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.domain.RolleType
import no.nav.amt_altinn_acl.service.AuthService
import no.nav.amt_altinn_acl.service.RolleService
import no.nav.amt_altinn_acl.utils.Issuer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rolle")
class RolleController(
	private val authService: AuthService,
	private val rolleService: RolleService
) {

	@Deprecated("Endepunktet vil snart bli slettet. Bytt til POST request",)
	@GetMapping("/tiltaksarrangor")
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	fun hentTiltaksarrangorRollerDeprecated(@RequestParam norskIdent: String): HentRollerResponse {
		authService.verifyRequestIsMachineToMachine()

		val roller = rolleService.getRollerForPerson(norskIdent)
		return HentRollerResponse(
			roller.map { rolle -> HentRollerResponse.TiltaksarrangorRoller(rolle.organisasjonsnummer, rolle.roller.map { it.rolleType }) }
		)
	}

	@PostMapping("/tiltaksarrangor")
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	fun hentTiltaksarrangorRoller(@RequestBody hentRollerRequest: HentRollerRequest): HentRollerResponse {
		authService.verifyRequestIsMachineToMachine()

		val roller = rolleService.getRollerForPerson(hentRollerRequest.personident)
		return HentRollerResponse(
			roller.map { rolle -> HentRollerResponse.TiltaksarrangorRoller(rolle.organisasjonsnummer, rolle.roller.map { it.rolleType }) }
		)
	}

	data class HentRollerRequest(val personident: String)

	data class HentRollerResponse(
		val roller: List<TiltaksarrangorRoller>
	) {
		data class TiltaksarrangorRoller(
			val organisasjonsnummer: String,
			val roller: List<RolleType>,
		)
	}


}

package no.nav.amt.altinn.acl.controller

import no.nav.amt.altinn.acl.domain.RolleType
import no.nav.amt.altinn.acl.service.AuthService
import no.nav.amt.altinn.acl.service.RolleService
import no.nav.amt.altinn.acl.utils.Issuer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rolle")
class RolleController(
	private val authService: AuthService,
	private val rolleService: RolleService,
) {
	@PostMapping("/tiltaksarrangor")
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	fun hentTiltaksarrangorRoller(
		@RequestBody hentRollerRequest: HentRollerRequest,
	): HentRollerResponse {
		authService.verifyRequestIsMachineToMachine()
		hentRollerRequest.validatePersonident()

		val tiltaksarrangorRoller =
			rolleService
				.getRollerForPerson(hentRollerRequest.personident)
				.map { rolle ->
					HentRollerResponse.TiltaksarrangorRoller(
						rolle.organisasjonsnummer,
						rolle.roller.map { it.rolleType },
					)
				}

		return HentRollerResponse(tiltaksarrangorRoller)
	}

	data class HentRollerRequest(
		val personident: String,
	) {
		fun validatePersonident() {
			if (personident.trim().length != 11 || !personident.trim().matches("""\d{11}""".toRegex())) {
				throw IllegalArgumentException("Ugyldig personident")
			}
		}
	}

	data class HentRollerResponse(
		val roller: List<TiltaksarrangorRoller>,
	) {
		data class TiltaksarrangorRoller(
			val organisasjonsnummer: String,
			val roller: List<RolleType>,
		)
	}
}

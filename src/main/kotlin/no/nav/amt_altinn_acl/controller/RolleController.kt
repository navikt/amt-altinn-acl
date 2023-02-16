package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.domain.Right
import no.nav.amt_altinn_acl.domain.RightType
import no.nav.amt_altinn_acl.domain.TiltaksarrangorRolleType
import no.nav.amt_altinn_acl.service.AuthService
import no.nav.amt_altinn_acl.service.RightsService
import no.nav.amt_altinn_acl.service.RolleService
import no.nav.amt_altinn_acl.utils.Issuer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rolle")
class RolleController(
	private val authService: AuthService,
	private val rolleService: RolleService,
	private val rightsService: RightsService
) {

	@GetMapping("/tiltaksarrangor")
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	fun hentTiltaksarrangorRoller(@RequestParam norskIdent: String): HentRollerResponse {
		authService.verifyRequestIsMachineToMachine()

		val rights = rightsService.getRightsForPerson(norskIdent)
		return HentRollerResponse(
			rights.map { HentRollerResponse.TiltaksarrangorRoller(it.organizationNumber, it.rights.map { it.toDto() }) }
		)
	}

	data class HentRollerResponse(
		val roller: List<TiltaksarrangorRoller>
	) {
		data class TiltaksarrangorRoller(
			val organisasjonsnummer: String,
			val roller: List<TiltaksarrangorRolleType>,
		)
	}

}

private fun Right.toDto(): TiltaksarrangorRolleType {
	return when(this.rightType) {
		RightType.KOORDINATOR -> TiltaksarrangorRolleType.KOORDINATOR
		RightType.VEILEDER -> TiltaksarrangorRolleType.VEILEDER
	}
}

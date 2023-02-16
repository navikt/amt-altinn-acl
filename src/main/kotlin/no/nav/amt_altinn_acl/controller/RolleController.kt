package no.nav.amt_altinn_acl.controller

import no.nav.amt_altinn_acl.domain.RoleType
import no.nav.amt_altinn_acl.service.AuthService
import no.nav.amt_altinn_acl.service.RoleService
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
	private val roleService: RoleService
) {

	@GetMapping("/tiltaksarrangor")
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	fun hentTiltaksarrangorRoller(@RequestParam norskIdent: String): HentRollerResponse {
		authService.verifyRequestIsMachineToMachine()

		val roles = roleService.getRolesForPerson(norskIdent)
		return HentRollerResponse(
			roles.map { right -> HentRollerResponse.TiltaksarrangorRoller(right.organizationNumber, right.roles.map { it.roleType }) }
		)
	}

	data class HentRollerResponse(
		val roller: List<TiltaksarrangorRoller>
	) {
		data class TiltaksarrangorRoller(
			val organisasjonsnummer: String,
			val roller: List<RoleType>,
		)
	}

}

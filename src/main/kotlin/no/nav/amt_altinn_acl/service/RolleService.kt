package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.domain.TiltaksarrangorRolleType
import no.nav.amt_altinn_acl.domain.TiltaksarrangorRoller
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RolleService(
	@Value("\${altinn.koordinator-service-code}") private val altinnKoordinatorServiceCode: String,
	@Value("\${altinn.veileder-service-code}") private val altinnVeilederServiceCode: String,
	private val rettigheterService: RettigheterService
) {

	fun hentTiltaksarrangorRoller(norskIdent: String): List<TiltaksarrangorRoller> {
		val rettigheter = rettigheterService.hentAlleRettigheter(norskIdent)

		return hentTiltaksarrangorRoller(rettigheter)
	}

	private fun hentTiltaksarrangorRoller(rettigheter: List<AltinnRettighet>): List<TiltaksarrangorRoller> {
		val altinnRollerMap = mutableMapOf<String, MutableList<TiltaksarrangorRolleType>>()

		rettigheter.forEach {
			val rolle = mapAltinnRettighetTilRolle(it.serviceCode) ?: return@forEach

			val roller = altinnRollerMap.computeIfAbsent(it.organisasjonsnummer) { mutableListOf() }

			roller.add(rolle)
		}

		return altinnRollerMap.map {
			TiltaksarrangorRoller(it.key, it.value)
		}
	}

	private fun mapAltinnRettighetTilRolle(rettighetId: String): TiltaksarrangorRolleType? {
		return when(rettighetId) {
			altinnKoordinatorServiceCode -> TiltaksarrangorRolleType.KOORDINATOR
			altinnVeilederServiceCode -> TiltaksarrangorRolleType.VEILEDER
			else -> null
		}
	}

}

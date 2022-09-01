package no.nav.amt_altinn_acl.service

import no.nav.amt_altinn_acl.domain.AltinnRettighet
import no.nav.amt_altinn_acl.domain.TiltaksarrangorRoller
import no.nav.amt_altinn_acl.domain.TiltaksarrangorRolleType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RolleService(
	@Value("\${altinn.koordinator-rettighet-id}") private val altinnKoordinatorRettighetId: String,
	private val rettigheterService: RettigheterService
) {

	fun hentTiltaksarrangorRoller(norskIdent: String): List<TiltaksarrangorRoller> {
		val rettigheter = rettigheterService.hentAlleRettigheter(norskIdent)

		return hentTiltaksarrangorRoller(rettigheter)
	}

	private fun hentTiltaksarrangorRoller(rettigheter: List<AltinnRettighet>): List<TiltaksarrangorRoller> {
		val altinnRollerMap = mutableMapOf<String, MutableList<TiltaksarrangorRolleType>>()

		rettigheter.forEach {
			val rolle = mapAltinnRettighetTilRolle(it.rettighetId) ?: return@forEach

			val roller = altinnRollerMap.computeIfAbsent(it.organisasjonsnummmer) { mutableListOf() }

			roller.add(rolle)
		}

		return altinnRollerMap.map {
			TiltaksarrangorRoller(it.key, it.value)
		}
	}

	private fun mapAltinnRettighetTilRolle(rettighetId: String): TiltaksarrangorRolleType? {
		return when(rettighetId) {
			altinnKoordinatorRettighetId -> TiltaksarrangorRolleType.KOORDINATOR
			else -> null
		}
	}

}

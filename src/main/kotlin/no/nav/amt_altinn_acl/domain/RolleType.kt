package no.nav.amt_altinn_acl.domain

enum class RolleType(val serviceCode: String, val resourceId: String) {
	KOORDINATOR("5858", "nav_tiltaksarrangor_deltakeroversikt-koordinator"),
	VEILEDER("5859", "nav_tiltaksarrangor_deltakeroversikt-veileder");

	companion object {
		fun fromServiceCode(code: String) = when (code) {
			KOORDINATOR.serviceCode -> KOORDINATOR
			VEILEDER.serviceCode -> VEILEDER
			else -> throw IllegalArgumentException("Ukjent servicekode: $code")
		}

		fun fromResourceId(id: String) = when (id) {
			KOORDINATOR.resourceId -> KOORDINATOR
			VEILEDER.resourceId -> VEILEDER
			else -> throw IllegalArgumentException("Ukjent resurssid $this")
		}
	}
}

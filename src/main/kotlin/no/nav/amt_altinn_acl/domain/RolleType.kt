package no.nav.amt_altinn_acl.domain

enum class RolleType(val serviceCode: String) {
	KOORDINATOR("5858"),
	VEILEDER("5859");

	companion object {
		fun fromServiceCode(code: String) = when (code) {
			"5858" -> KOORDINATOR
			"5859" -> VEILEDER
			else -> throw IllegalArgumentException("Ukjent servicekode: $code")
		}
	}
}

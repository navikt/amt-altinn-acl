package no.nav.amt_altinn_acl.domain

data class TiltaksarrangorRoller(
	val organisasjonsnummmer: String,
	val roller: List<TiltaksarrangorRolleType>,
)

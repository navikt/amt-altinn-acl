package no.nav.amt_altinn_acl.client.unleash;

import io.getunleash.Unleash
import org.springframework.stereotype.Component;

@Component
class UnleashClient (
	private val unleash: Unleash,
) {
	companion object {
		const val ALTINN3_TOGGLE = "amt-altinn3-toggle"
	}

	fun erAltinn3Enabled() = unleash.isEnabled(ALTINN3_TOGGLE)
}

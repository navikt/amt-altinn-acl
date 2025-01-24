package no.nav.amt_altinn_acl.client.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("default")
@Configuration
class UnleashConfig {
	@Bean
	fun defaultUnleash(
		@Value("\${app.env.unleashUrl}") unleashUrl: String,
		@Value("\${app.env.unleashApiToken}") unleashApiToken: String,
	): DefaultUnleash {
		val appName = "amt-altinn-acl"
		val config =
			UnleashConfig
				.builder()
				.appName(appName)
				.instanceId(appName)
				.unleashAPI(unleashUrl)
				.apiKey(unleashApiToken)
				.build()
		return DefaultUnleash(config)
	}
}

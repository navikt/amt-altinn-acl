package no.nav.amt.altinn.acl.config

import no.nav.amt.lib.utils.leaderelection.Leader
import no.nav.amt.lib.utils.leaderelection.LeaderElectionClient
import no.nav.amt.lib.utils.leaderelection.LeaderProvider
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.filter.LogRequestFilter
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableJwtTokenValidation
class ApplicationConfig(
	@Value($$"${elector.path}") private val electorPath: String,
	private val objectMapper: ObjectMapper,
) {
	private val httpClient: OkHttpClient = RestClient.baseClient()

	@Bean
	fun leaderElectionClient(): LeaderElectionClient {
		val leaderProvider =
			LeaderProvider { path ->
				val request =
					Request
						.Builder()
						.url(if (path.startsWith("http://")) path else "http://$path")
						.get()
						.build()

				httpClient.newCall(request).execute().use { response ->
					if (!response.isSuccessful) {
						throw RuntimeException("Kall mot elector feiler med HTTP-${response.code}")
					}
					objectMapper.readValue(response.body.string(), Leader::class.java)
				}
			}

		return LeaderElectionClient(leaderProvider, electorPath)
	}

	@Bean
	fun logFilterRegistrationBean(): FilterRegistrationBean<LogRequestFilter> =
		FilterRegistrationBean<LogRequestFilter>().apply {
			@Suppress("UsePropertyAccessSyntax")
			setFilter(LogRequestFilter("amt-altinn-acl", false))
			order = 1
			addUrlPatterns("/*")
		}
}

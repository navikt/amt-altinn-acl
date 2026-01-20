package no.nav.amt_altinn_acl.config

import no.nav.common.rest.filter.LogRequestFilter
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableJwtTokenValidation
class ApplicationConfig {
	@Bean
	fun logFilterRegistrationBean(): FilterRegistrationBean<LogRequestFilter> =
		FilterRegistrationBean<LogRequestFilter>().apply {
			@Suppress("UsePropertyAccessSyntax")
			setFilter(LogRequestFilter("amt-altinn-acl", false))
			order = 1
			addUrlPatterns("/*")
		}
}

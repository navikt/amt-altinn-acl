package no.nav.amt_altinn_acl.config

import no.nav.common.log.LogFilter
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJwtTokenValidation
class ApplicationConfig {

	@Bean
	fun logFilterRegistrationBean(): FilterRegistrationBean<LogFilter> {
		val registration = FilterRegistrationBean<LogFilter>()
		registration.filter = LogFilter("amt-altinn-acl", false)
		registration.order = 1
		registration.addUrlPatterns("/*")
		return registration
	}

}

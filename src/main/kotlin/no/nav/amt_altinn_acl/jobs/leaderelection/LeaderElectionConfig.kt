package no.nav.amt_altinn_acl.jobs.leaderelection

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LeaderElectionConfig {
	@Value("\${elector.path}")
	lateinit var electorPath: String

	@Bean
	fun leaderElection(): LeaderElection {
		return LeaderElection(
			electorPath = electorPath
		)
	}
}

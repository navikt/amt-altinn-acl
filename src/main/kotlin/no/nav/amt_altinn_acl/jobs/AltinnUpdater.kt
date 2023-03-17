package no.nav.amt_altinn_acl.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.amt_altinn_acl.jobs.leaderelection.LeaderElection
import no.nav.amt_altinn_acl.service.RolleService
import no.nav.common.job.JobRunner
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled

@Configuration
class AltinnUpdater(
	private val rolleService: RolleService,
	private val leaderElection: LeaderElection
) {

	@Scheduled(cron = "@hourly")
	@SchedulerLock(name = "synkroniser_altinn_rettigheter", lockAtMostFor = "120m")
	fun update() {
		if (leaderElection.isLeader()) {
			JobRunner.run("synkroniser_altinn_rettigheter") {
				rolleService.synchronizeUsers()
			}
		}
	}
}

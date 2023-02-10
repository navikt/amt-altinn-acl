package no.nav.amt_altinn_acl.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.amt_altinn_acl.service.RettigheterService
import no.nav.common.job.JobRunner
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled

@Configuration
class AltinnUpdater(
	private val rettigheterService: RettigheterService
) {

	@Scheduled(cron = "@hourly")
	@SchedulerLock(name = "synkroniser_altinn_rettigheter", lockAtMostFor = "120m")
	fun update() {
		JobRunner.run("synkroniser_altinn_rettigheter") {
			rettigheterService
				.hentUtdaterteBrukere(batchSize = 25)
				.map { it.personligIdent }
				.also {
					rettigheterService.synkroniserBrukere(it)
				}

		}
	}
}

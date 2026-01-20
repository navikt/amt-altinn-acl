package no.nav.amt_altinn_acl.testutil

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestConfig {
	@Bean
	fun machineToMachineTokenClient() = MachineToMachineTokenClient { "TOKEN" }
}

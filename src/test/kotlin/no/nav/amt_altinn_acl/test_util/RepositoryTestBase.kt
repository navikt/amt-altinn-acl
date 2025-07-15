package no.nav.amt_altinn_acl.test_util

import no.nav.amt_altinn_acl.test_util.DbTestDataUtils.cleanDatabase
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

@ActiveProfiles("test")
@AutoConfigureJdbc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class RepositoryTestBase {
	@Autowired
	private lateinit var dataSource: DataSource

	@AfterEach
	fun cleanDatabase() = cleanDatabase(dataSource)

	companion object {
		private const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:14-alpine"

		@ServiceConnection
		@Suppress("unused")
		private val container = PostgreSQLContainer<Nothing>(POSTGRES_DOCKER_IMAGE_NAME)
	}
}

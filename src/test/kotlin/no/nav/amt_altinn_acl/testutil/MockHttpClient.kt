package no.nav.amt_altinn_acl.testutil

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.slf4j.LoggerFactory

open class MockHttpClient {
	private val server = MockWebServer()
	private val log = LoggerFactory.getLogger(javaClass)

	fun start() {
		try {
			server.start()
		} catch (_: IllegalArgumentException) {
			log.info("${javaClass.simpleName} is already started")
		}
	}

	fun serverUrl(): String = server.url("").toString().removeSuffix("/")

	fun enqueue(
		responseCode: Int = 200,
		headers: Map<String, String> = emptyMap(),
		body: String,
	) {
		val response =
			MockResponse()
				.setBody(body)
				.setResponseCode(responseCode)

		headers.forEach {
			response.addHeader(it.key, it.value)
		}

		server.enqueue(response)
	}
}

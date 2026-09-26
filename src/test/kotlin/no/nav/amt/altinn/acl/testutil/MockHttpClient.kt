package no.nav.amt.altinn.acl.testutil

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue

open class MockHttpClient {
    private val server = HttpServer.create(InetSocketAddress(0), 0)
    private val responses = ConcurrentLinkedQueue<MockResponse>()
    private val requests = ConcurrentLinkedQueue<RecordedRequest>()

    init {
        server.createContext("/") { exchange ->
            requests.add(
                RecordedRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.path,
                    body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() },
                ),
            )
            val response = responses.poll() ?: error("Mock has no queued response")
            response.headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
            val body = response.body.toByteArray()
            exchange.sendResponseHeaders(response.statusCode, if (body.isEmpty()) -1L else body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
    }

    fun start() = server.start()

    fun stop() = server.stop(0)

    fun serverUrl(): String = "http://localhost:${server.address.port}"

    fun takeRequest(): RecordedRequest = requests.poll() ?: error("Mock has no recorded request")

    fun enqueue(
        responseCode: Int = 200,
        headers: Map<String, String> = emptyMap(),
        body: String,
    ) {
        responses.add(MockResponse(responseCode, headers, body))
    }

    private data class MockResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: String,
    )

    data class RecordedRequest(
        val method: String,
        val path: String,
        val body: String,
    )
}

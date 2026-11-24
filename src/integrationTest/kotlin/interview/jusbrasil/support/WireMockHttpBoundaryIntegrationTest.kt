package interview.jusbrasil.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration

/**
 * Reference pattern for outbound HTTP boundaries.
 * Production code does not call an external API yet. Keep WireMock at the HTTP
 * boundary when one is introduced, and do not depend on a real third-party API.
 */
class WireMockHttpBoundaryIntegrationTest {
    @Test
    fun `stubs a successful response and verifies the outbound request`() {
        withServer({
            stubFor(
                get(urlEqualTo("/external/status"))
                    .willReturn(
                        aResponse()
                            .withStatus(STATUS_OK)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""{"status":"UP"}"""),
                    ),
            )
        }) { server ->
            val response = send(server, "/external/status", Duration.ofSeconds(2))

            assertEquals(STATUS_OK, response.statusCode())
            assertEquals("""{"status":"UP"}""", response.body())
            server.verify(
                getRequestedFor(urlEqualTo("/external/status"))
                    .withHeader("Accept", equalTo("application/json"))
                    .withHeader("X-Request-Id", equalTo("boundary-test")),
            )
        }
    }

    @Test
    fun `exposes an http error and an invalid payload from the external service`() {
        withServer({
            stubFor(
                get(urlEqualTo("/external/status"))
                    .willReturn(
                        aResponse()
                            .withStatus(STATUS_UNAVAILABLE)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("not-json"),
                    ),
            )
        }) { server ->
            val response = send(server, "/external/status", Duration.ofSeconds(2))

            assertEquals(STATUS_UNAVAILABLE, response.statusCode())
            assertEquals("not-json", response.body())
        }
    }

    @Test
    fun `fails when the external service does not answer in time`() {
        withServer({
            stubFor(
                get(urlEqualTo("/external/status"))
                    .willReturn(
                        aResponse()
                            .withStatus(STATUS_OK)
                            .withFixedDelay(SLOW_RESPONSE_DELAY_MILLIS),
                    ),
            )
        }) { server ->
            assertThrows(HttpTimeoutException::class.java) {
                send(server, "/external/status", Duration.ofMillis(SHORT_TIMEOUT_MILLIS))
            }
        }
    }

    private fun send(
        server: WireMockServer,
        path: String,
        timeout: Duration,
    ): HttpResponse<String> {
        val client =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()
        val request =
            HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:${server.port()}$path"))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("X-Request-Id", "boundary-test")
                .GET()
                .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun withServer(
        configure: WireMockServer.() -> Unit,
        block: (WireMockServer) -> Unit,
    ) {
        val server =
            WireMockServer(
                WireMockConfiguration
                    .options()
                    .dynamicPort()
                    .bindAddress("127.0.0.1"),
            )
        server.start()
        try {
            server.configure()
            block(server)
        } finally {
            server.stop()
        }
    }

    private companion object {
        const val STATUS_OK = 200
        const val STATUS_UNAVAILABLE = 503
        const val SLOW_RESPONSE_DELAY_MILLIS = 5_000
        const val SHORT_TIMEOUT_MILLIS = 200L
    }
}

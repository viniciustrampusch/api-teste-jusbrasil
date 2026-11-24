package interview.jusbrasil.shorturl

import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class ShortUrlEndpointIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val store: InMemoryShortLinkStore,
) {
    @Test
    fun `creates a short link with a 7 character code`() {
        val response =
            mockMvc
                .perform(
                    post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"url":"https://example.com/docs"}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.shortCode").value(matchesPattern("[0-9A-Za-z]{7}")))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/docs"))
                .andExpect(jsonPath("$.expiresAt").value(nullValue()))
                .andReturn()
                .response

        val code = shortCode(response.contentAsString)
        assertEquals(true, response.contentAsString.contains(""""shortUrl":""") && response.contentAsString.contains("/$code"))
    }

    @Test
    fun `rejects a url that is not http or https`() {
        mockMvc
            .perform(
                post("/api/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"url":"ftp://example.com/file"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `redirects and records referrer user agent and ip`() {
        val created =
            mockMvc
                .perform(
                    post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"url":"https://example.com/target"}"""),
                ).andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString
        val code = shortCode(created)

        mockMvc
            .perform(
                get("/$code")
                    .header("Referer", "https://ref.example")
                    .header("User-Agent", "TestAgent")
                    .with(remoteAddress("203.0.113.5")),
            ).andExpect(status().isMovedPermanently)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Location", "https://example.com/target"))

        val click = store.clicksOf(code).single()
        assertEquals("https://ref.example", click.referrer)
        assertEquals("TestAgent", click.userAgent)
        assertEquals("203.0.113.5", click.ip)
    }

    @Test
    fun `counts every request for the same code`() {
        val created =
            mockMvc
                .perform(
                    post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"url":"https://example.com/repeat"}"""),
                ).andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString
        val code = shortCode(created)

        repeat(2) {
            mockMvc
                .perform(get("/$code"))
                .andExpect(status().isMovedPermanently)
                .andExpect(header().string("Cache-Control", "no-store"))
        }

        assertEquals(2, store.clicksOf(code).size)
        mockMvc
            .perform(get("/api/stats/$code"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalClicks").value(2))
    }

    @Test
    fun `returns not found for an unknown code`() {
        mockMvc.perform(get("/missing")).andExpect(status().isNotFound)
    }

    @Test
    fun `returns gone for an expired link and records no click`() {
        val url = OriginalUrl.parse("https://example.com/expired") ?: error("url")
        check(
            store.insert(
                ShortLink(
                    code = "expired",
                    originalUrl = url,
                    createdAt = Instant.parse("2020-01-01T00:00:00Z"),
                    expiresAt = Instant.parse("2020-01-02T00:00:00Z"),
                ),
            ),
        )

        mockMvc.perform(get("/expired")).andExpect(status().isGone)

        assertEquals(emptyList<Click>(), store.clicksOf("expired"))
    }

    private fun shortCode(body: String): String =
        Regex(""""shortCode"\s*:\s*"([0-9A-Za-z]{7})"""")
            .find(body)
            ?.groupValues
            ?.get(1)
            ?: error("shortCode missing from response")

    private fun remoteAddress(ip: String) =
        RequestPostProcessor { request ->
            request.remoteAddr = ip
            request
        }
}

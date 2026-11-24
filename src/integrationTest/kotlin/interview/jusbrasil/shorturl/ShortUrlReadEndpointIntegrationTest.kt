package interview.jusbrasil.shorturl

import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ShortUrlReadEndpointIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `lists the newest link first with its click count`() {
        val older = create("https://example.com/older")
        val newer = create("https://example.com/newer")
        mockMvc
            .perform(get("/$newer").header("Referer", "https://ref.example"))
            .andExpect(status().isMovedPermanently)

        val body =
            mockMvc
                .perform(get("/api/urls"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[?(@.shortCode == '$newer')].clickCount").value(contains(1)))
                .andExpect(jsonPath("$[?(@.shortCode == '$older')].clickCount").value(contains(0)))
                .andReturn()
                .response
                .contentAsString
        val codes = Regex(""""shortCode"\s*:\s*"([0-9A-Za-z]{7})"""").findAll(body).map { it.groupValues[1] }.toList()

        assertTrue(codes.indexOf(newer) < codes.indexOf(older))
    }

    @Test
    fun `returns click stats for an existing code`() {
        val code = create("https://example.com/stats")
        mockMvc
            .perform(get("/$code").header("Referer", "https://ref.example"))
            .andExpect(status().isMovedPermanently)

        mockMvc
            .perform(get("/api/stats/$code"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalClicks").value(1))
            .andExpect(jsonPath("$.clicksByDay.length()").value(STATS_DAYS))
            .andExpect(jsonPath("$.clicksByDay[?(@.clicks > 0)].date").isNotEmpty)
            .andExpect(jsonPath("$.topReferrers[0].referrer").value("https://ref.example"))
            .andExpect(jsonPath("$.topReferrers[0].clicks").value(1))
    }

    @Test
    fun `returns not found stats for an unknown code`() {
        mockMvc
            .perform(get("/api/stats/nope123"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Short code not found"))
    }

    private fun create(url: String): String {
        val body =
            mockMvc
                .perform(
                    post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"url":"$url"}"""),
                ).andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString
        return Regex(""""shortCode"\s*:\s*"([0-9A-Za-z]{7})"""")
            .find(body)
            ?.groupValues
            ?.get(1)
            ?: error("shortCode missing from response")
    }

    private companion object {
        const val STATS_DAYS = 30
    }
}

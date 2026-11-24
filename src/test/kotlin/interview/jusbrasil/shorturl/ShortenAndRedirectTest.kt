package interview.jusbrasil.shorturl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShortenUrlTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `retries a taken code and stores the next free code`() {
        val store = InMemoryShortLinkStore()
        seed(store, "aaaaaaa")
        val codes = ArrayDeque(listOf("aaaaaaa", "aaaaaaa", "bbbbbbb"))
        val shorten = ShortenUrl(store, ShortCodeGenerator { codes.removeFirst() }, clock)

        val result = shorten.execute("https://example.com/docs", expiresAt = null)

        val created = result as ShortenResult.Created
        assertEquals("bbbbbbb", created.link.code)
        assertEquals("https://example.com/docs", store.find("bbbbbbb")?.originalUrl?.value)
        assertEquals("https://example.com", store.find("aaaaaaa")?.originalUrl?.value)
    }

    @Test
    fun `rejects an expiration that is not in the future`() {
        val store = InMemoryShortLinkStore()
        val shorten = ShortenUrl(store, ShortCodeGenerator { "aaaaaaa" }, clock)

        val result = shorten.execute("https://example.com", expiresAt = now)

        assertTrue(result is ShortenResult.Invalid)
        assertEquals(null, store.find("aaaaaaa"))
    }

    private fun seed(
        store: InMemoryShortLinkStore,
        code: String,
    ) {
        val existing =
            ShortLink(
                code = code,
                originalUrl = OriginalUrl.parse("https://example.com")!!,
                createdAt = now,
                expiresAt = null,
            )
        check(store.insert(existing))
    }
}

class RedirectUrlTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")
    private val url = OriginalUrl.parse("https://example.com/docs")!!
    private val click =
        ClickRequest(
            referrer = "https://ref.example",
            userAgent = "TestAgent",
            ip = "203.0.113.5",
        )

    @Test
    fun `redirects and records the click`() {
        val store = storeWith(expiresAt = now.plusSeconds(60))
        val redirect = RedirectUrl(store, Clock.fixed(now, ZoneOffset.UTC))

        val result = redirect.execute("aaaaaaa", click)

        assertEquals(RedirectResult.Moved("https://example.com/docs"), result)
        val stored = store.clicksOf("aaaaaaa").single()
        assertEquals(now, stored.clickedAt)
        assertEquals("https://ref.example", stored.referrer)
        assertEquals("TestAgent", stored.userAgent)
        assertEquals("203.0.113.5", stored.ip)
    }

    @Test
    fun `records a separate click on every redirect of the same code`() {
        val store = storeWith(expiresAt = null)
        val redirect = RedirectUrl(store, Clock.fixed(now, ZoneOffset.UTC))

        redirect.execute("aaaaaaa", click)
        redirect.execute("aaaaaaa", click)

        assertEquals(2, store.clicksOf("aaaaaaa").size)
    }

    @Test
    fun `returns not found when the code is missing`() {
        val store = InMemoryShortLinkStore()
        val redirect = RedirectUrl(store, Clock.fixed(now, ZoneOffset.UTC))

        val result = redirect.execute("missing", click)

        assertEquals(RedirectResult.NotFound, result)
        assertEquals(emptyList<Click>(), store.clicksOf("missing"))
    }

    @Test
    fun `returns gone at the exact expiration instant and records no click`() {
        val store = storeWith(expiresAt = now)
        val redirect = RedirectUrl(store, Clock.fixed(now, ZoneOffset.UTC))

        val result = redirect.execute("aaaaaaa", click)

        assertEquals(RedirectResult.Gone, result)
        assertEquals(emptyList<Click>(), store.clicksOf("aaaaaaa"))
    }

    @Test
    fun `redirects when expiration is still in the future`() {
        val store = storeWith(expiresAt = now.plusSeconds(1))
        val redirect = RedirectUrl(store, Clock.fixed(now, ZoneOffset.UTC))

        val result = redirect.execute("aaaaaaa", click)

        assertEquals(RedirectResult.Moved("https://example.com/docs"), result)
    }

    @Test
    fun `redirects a link without expiration after the clock moves forward`() {
        val store = storeWith(expiresAt = null)
        val later = Clock.fixed(now.plusSeconds(86_400), ZoneOffset.UTC)
        val redirect = RedirectUrl(store, later)

        val result = redirect.execute("aaaaaaa", click)

        assertEquals(RedirectResult.Moved("https://example.com/docs"), result)
    }

    private fun storeWith(expiresAt: Instant?): InMemoryShortLinkStore {
        val store = InMemoryShortLinkStore()
        check(
            store.insert(
                ShortLink(
                    code = "aaaaaaa",
                    originalUrl = url,
                    createdAt = now,
                    expiresAt = expiresAt,
                ),
            ),
        )
        return store
    }
}

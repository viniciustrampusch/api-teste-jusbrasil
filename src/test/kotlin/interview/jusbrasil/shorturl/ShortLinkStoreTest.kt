package interview.jusbrasil.shorturl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class OriginalUrlTest {
    @Test
    fun `accepts http and https urls with a host`() {
        assertEquals("http://example.com", OriginalUrl.parse("http://example.com")?.value)
        assertEquals("https://example.com/docs", OriginalUrl.parse("https://example.com/docs")?.value)
        assertEquals("HTTP://example.com", OriginalUrl.parse("HTTP://example.com")?.value)
    }

    @Test
    fun `rejects a scheme other than http or https`() {
        assertNull(OriginalUrl.parse("ftp://example.com/file"))
    }

    @Test
    fun `rejects a relative url`() {
        assertNull(OriginalUrl.parse("/docs"))
    }

    @Test
    fun `rejects a url without a host`() {
        assertNull(OriginalUrl.parse("http://"))
        assertNull(OriginalUrl.parse("not a url"))
    }
}

class ShortCodeGeneratorTest {
    @Test
    fun `emits a 7 character alphanumeric code`() {
        val generator = RandomShortCodeGenerator(java.util.Random(1))

        repeat(20) {
            val code = generator.next()

            assertEquals(ShortCodes.LENGTH, code.length)
            assertEquals(true, ShortCodes.isValid(code))
        }
    }
}

class InMemoryShortLinkStoreTest {
    private val store = InMemoryShortLinkStore()
    private val url = OriginalUrl.parse("https://example.com")!!

    @Test
    fun `second insert of the same code returns false and keeps the first link`() {
        val first = link("aaaaaaa")
        val second = link("aaaaaaa", url = OriginalUrl.parse("https://other.example")!!)

        assertEquals(true, store.insert(first))
        assertEquals(false, store.insert(second))
        assertEquals(first, store.find("aaaaaaa"))
    }

    @Test
    fun `find returns null when the code is missing`() {
        assertNull(store.find("missing"))
    }

    @Test
    fun `click for an unknown code fails and stores nothing`() {
        val click = click("missing")

        assertThrows(UnknownShortCodeException::class.java) {
            store.insertClick(click)
        }
        assertEquals(emptyList<Click>(), store.clicksOf("missing"))
    }

    @Test
    fun `two clicks keep increasing ids`() {
        store.insert(link("aaaaaaa"))

        val first = store.insertClick(click("aaaaaaa"))
        val second = store.insertClick(click("aaaaaaa"))

        assertEquals(1L, first.id)
        assertEquals(2L, second.id)
        assertEquals(listOf(first, second), store.clicksOf("aaaaaaa"))
    }

    private fun link(
        code: String,
        url: OriginalUrl = this.url,
    ) = ShortLink(
        code = code,
        originalUrl = url,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        expiresAt = null,
    )

    private fun click(code: String) =
        NewClick(
            code = code,
            clickedAt = Instant.parse("2026-01-01T00:00:00Z"),
            referrer = "",
            userAgent = "",
            ip = "127.0.0.1",
        )
}

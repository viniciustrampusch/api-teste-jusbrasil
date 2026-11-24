package interview.jusbrasil.shorturl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ListUrlsTest {
    private val olderInstant = Instant.parse("2026-06-01T00:00:00Z")
    private val newerInstant = Instant.parse("2026-06-02T00:00:00Z")

    @Test
    fun `lists the newest link first and counts its clicks`() {
        val store = InMemoryShortLinkStore()
        insert(store, "aaaaaaa", olderInstant)
        insert(store, "bbbbbbb", newerInstant)
        store.insertClick(click("bbbbbbb", newerInstant))
        store.insertClick(click("bbbbbbb", newerInstant))

        val listed = ListUrls(store).execute()

        assertEquals(listOf("bbbbbbb", "aaaaaaa"), listed.map { it.link.code })
        assertEquals(2, listed.first().clickCount)
        assertEquals(0, listed.last().clickCount)
    }

    @Test
    fun `breaks a creation time tie by short code`() {
        val store = InMemoryShortLinkStore()
        insert(store, "bbbbbbb", olderInstant)
        insert(store, "aaaaaaa", olderInstant)

        val listed = ListUrls(store).execute()

        assertEquals(listOf("aaaaaaa", "bbbbbbb"), listed.map { it.link.code })
    }

    private fun insert(
        store: InMemoryShortLinkStore,
        code: String,
        createdAt: Instant,
    ) {
        check(
            store.insert(
                ShortLink(
                    code = code,
                    originalUrl = OriginalUrl.parse("https://example.com/$code")!!,
                    createdAt = createdAt,
                    expiresAt = null,
                ),
            ),
        )
    }

    private fun click(
        code: String,
        clickedAt: Instant,
    ) = NewClick(
        code = code,
        clickedAt = clickedAt,
        referrer = "https://ref.example",
        userAgent = "TestAgent",
        ip = "203.0.113.5",
    )
}

class GetStatsTest {
    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val today = LocalDate.parse("2026-06-15")
    private val windowStart = LocalDate.parse("2026-05-17")

    @Test
    fun `returns not found when the code does not exist`() {
        val stats = GetStats(InMemoryShortLinkStore(), clock)

        assertEquals(StatsResult.NotFound, stats.execute("missing"))
    }

    @Test
    fun `counts every click in the total and only the 30 day window in the series`() {
        val store = InMemoryShortLinkStore()
        insert(store, "aaaaaaa")
        store.insertClick(click("aaaaaaa", Instant.parse("2026-06-15T00:00:00Z"), "https://ref.example"))
        store.insertClick(click("aaaaaaa", Instant.parse("2026-05-16T23:00:00Z"), "https://old.example"))
        val stats = GetStats(store, clock).execute("aaaaaaa") as StatsResult.Found

        assertEquals(2, stats.stats.totalClicks)
        assertEquals(STATS_DAYS, stats.stats.clicksByDay.size)
        assertEquals(
            windowStart,
            stats.stats.clicksByDay
                .first()
                .date,
        )
        assertEquals(
            today,
            stats.stats.clicksByDay
                .last()
                .date,
        )
        assertEquals(
            1,
            stats.stats.clicksByDay
                .last()
                .clicks,
        )
        assertEquals(
            0,
            stats.stats.clicksByDay
                .first()
                .clicks,
        )
        assertEquals(
            listOf(ReferrerCount("https://old.example", 1), ReferrerCount("https://ref.example", 1)),
            stats.stats.topReferrers,
        )
    }

    @Test
    fun `keeps the five referrers with the most clicks and breaks ties by name`() {
        val store = InMemoryShortLinkStore()
        insert(store, "aaaaaaa")
        listOf("b" to 2, "a" to 2, "c" to 1, "d" to 1, "e" to 1, "f" to 1).forEach { (referrer, times) ->
            repeat(times) {
                store.insertClick(click("aaaaaaa", now, referrer))
            }
        }

        val stats = GetStats(store, clock).execute("aaaaaaa") as StatsResult.Found

        assertEquals(
            listOf(
                ReferrerCount("a", 2),
                ReferrerCount("b", 2),
                ReferrerCount("c", 1),
                ReferrerCount("d", 1),
                ReferrerCount("e", 1),
            ),
            stats.stats.topReferrers,
        )
    }

    private fun insert(
        store: InMemoryShortLinkStore,
        code: String,
    ) {
        check(
            store.insert(
                ShortLink(
                    code = code,
                    originalUrl = OriginalUrl.parse("https://example.com")!!,
                    createdAt = now,
                    expiresAt = null,
                ),
            ),
        )
    }

    private fun click(
        code: String,
        clickedAt: Instant,
        referrer: String,
    ) = NewClick(
        code = code,
        clickedAt = clickedAt,
        referrer = referrer,
        userAgent = "TestAgent",
        ip = "203.0.113.5",
    )

    private companion object {
        const val STATS_DAYS = 30
    }
}

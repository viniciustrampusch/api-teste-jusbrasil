package interview.jusbrasil.shorturl

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

data class DayClicks(
    val date: LocalDate,
    val clicks: Int,
)

data class ReferrerCount(
    val referrer: String,
    val clicks: Int,
)

data class LinkStats(
    val shortCode: String,
    val totalClicks: Int,
    val clicksByDay: List<DayClicks>,
    val topReferrers: List<ReferrerCount>,
)

sealed interface StatsResult {
    data class Found(
        val stats: LinkStats,
    ) : StatsResult

    data object NotFound : StatsResult
}

@Component
class GetStats(
    private val store: InMemoryShortLinkStore,
    private val clock: Clock,
) {
    fun execute(code: String): StatsResult {
        if (store.find(code) == null) {
            return StatsResult.NotFound
        }
        return StatsResult.Found(summarize(code))
    }

    private fun summarize(code: String): LinkStats {
        val clicks = store.clicksOf(code)
        val today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC)
        val start = today.minusDays(WINDOW_DAYS - 1L)
        val countsByDay =
            clicks
                .map { LocalDate.ofInstant(it.clickedAt, ZoneOffset.UTC) }
                .filter { !it.isBefore(start) && !it.isAfter(today) }
                .groupingBy { it }
                .eachCount()
        val series =
            (0 until WINDOW_DAYS).map { offset ->
                val date = start.plusDays(offset.toLong())
                DayClicks(date, countsByDay[date] ?: 0)
            }
        val topReferrers =
            clicks
                .groupingBy { it.referrer }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(TOP_REFERRERS)
                .map { ReferrerCount(it.key, it.value) }
        return LinkStats(code, clicks.size, series, topReferrers)
    }

    private companion object {
        const val WINDOW_DAYS = 30
        const val TOP_REFERRERS = 5
    }
}

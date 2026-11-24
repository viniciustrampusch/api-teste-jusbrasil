package interview.jusbrasil.shorturl

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter

data class ShortenRequest(
    val url: String,
    val expiresAt: Instant? = null,
)

data class ShortenResponse(
    val shortCode: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
)

data class ErrorResponse(
    val message: String,
)

data class ListedUrlResponse(
    val shortCode: String,
    val shortUrl: String,
    val originalUrl: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val clickCount: Int,
)

data class DayClicksResponse(
    val date: String,
    val clicks: Int,
)

data class ReferrerCountResponse(
    val referrer: String,
    val clicks: Int,
)

data class StatsResponse(
    val shortCode: String,
    val totalClicks: Int,
    val clicksByDay: List<DayClicksResponse>,
    val topReferrers: List<ReferrerCountResponse>,
)

@RestController
class ShortUrlController(
    private val shortenUrl: ShortenUrl,
    private val redirectUrl: RedirectUrl,
    private val listUrls: ListUrls,
    private val getStats: GetStats,
) {
    @PostMapping("/api/shorten")
    fun shorten(
        @RequestBody request: ShortenRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> =
        when (val result = shortenUrl.execute(request.url, request.expiresAt)) {
            is ShortenResult.Created -> {
                created(result.link, httpRequest)
            }

            is ShortenResult.Invalid -> {
                logger.info("shorten outcome=invalid")
                ResponseEntity.badRequest().body(ErrorResponse(result.message))
            }

            ShortenResult.Unavailable -> {
                logger.info("shorten outcome=unavailable")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Could not allocate a short code"))
            }
        }

    @GetMapping("/{code:[0-9A-Za-z]{7}}")
    fun redirect(
        @PathVariable code: String,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val result =
            redirectUrl.execute(
                code,
                ClickRequest(
                    referrer = request.getHeader("Referer").orEmpty(),
                    userAgent = request.getHeader("User-Agent").orEmpty(),
                    ip = request.remoteAddr.orEmpty(),
                ),
            )
        logger.info("short link code={} outcome={}", code, result.outcome())
        return when (result) {
            is RedirectResult.Moved -> {
                ResponseEntity
                    .status(HttpStatus.MOVED_PERMANENTLY)
                    .cacheControl(CacheControl.noStore())
                    .location(URI.create(result.location))
                    .build<Unit>()
            }

            RedirectResult.NotFound -> {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<Unit>()
            }

            RedirectResult.Gone -> {
                ResponseEntity.status(HttpStatus.GONE).build<Unit>()
            }
        }
    }

    @GetMapping("/api/urls")
    fun list(request: HttpServletRequest): List<ListedUrlResponse> {
        logger.info("list urls outcome=ok")
        return listUrls.execute().map { listed ->
            ListedUrlResponse(
                shortCode = listed.link.code,
                shortUrl = shortUrl(request, listed.link.code),
                originalUrl = listed.link.originalUrl.value,
                createdAt = listed.link.createdAt,
                expiresAt = listed.link.expiresAt,
                clickCount = listed.clickCount,
            )
        }
    }

    @GetMapping("/api/stats/{code:[0-9A-Za-z]{7}}")
    fun stats(
        @PathVariable code: String,
    ): ResponseEntity<Any> =
        when (val result = getStats.execute(code)) {
            is StatsResult.Found -> {
                logger.info("short link code={} outcome=stats", code)
                ResponseEntity.ok(statsResponse(result.stats))
            }

            StatsResult.NotFound -> {
                logger.info("short link code={} outcome=stats_not_found", code)
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Short code not found"))
            }
        }

    private fun created(
        link: ShortLink,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        logger.info("short link code={} outcome=created", link.code)
        val body =
            ShortenResponse(
                shortCode = link.code,
                shortUrl = shortUrl(request, link.code),
                originalUrl = link.originalUrl.value,
                createdAt = link.createdAt,
                expiresAt = link.expiresAt,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(body)
    }

    private fun shortUrl(
        request: HttpServletRequest,
        code: String,
    ): String =
        ServletUriComponentsBuilder
            .fromRequest(request)
            .replacePath(request.contextPath + "/" + code)
            .replaceQuery(null)
            .fragment(null)
            .build()
            .toUriString()

    private fun statsResponse(stats: LinkStats): StatsResponse =
        StatsResponse(
            shortCode = stats.shortCode,
            totalClicks = stats.totalClicks,
            clicksByDay =
                stats.clicksByDay.map { day ->
                    DayClicksResponse(day.date.format(DATE), day.clicks)
                },
            topReferrers =
                stats.topReferrers.map { referrer ->
                    ReferrerCountResponse(referrer.referrer, referrer.clicks)
                },
        )

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ShortUrlController::class.java)
        val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun RedirectResult.outcome(): String =
            when (this) {
                is RedirectResult.Moved -> "moved"
                RedirectResult.NotFound -> "not_found"
                RedirectResult.Gone -> "gone"
            }
    }
}

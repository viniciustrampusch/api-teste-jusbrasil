package interview.jusbrasil.shorturl

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

sealed interface ShortenResult {
    data class Created(
        val link: ShortLink,
    ) : ShortenResult

    data class Invalid(
        val message: String,
    ) : ShortenResult

    data object Unavailable : ShortenResult
}

@Component
class ShortenUrl(
    private val store: InMemoryShortLinkStore,
    private val codeGenerator: ShortCodeGenerator,
    private val clock: Clock,
) {
    fun execute(
        url: String,
        expiresAt: Instant?,
    ): ShortenResult {
        val originalUrl = OriginalUrl.parse(url)
        val now = clock.instant()
        return when {
            originalUrl == null -> ShortenResult.Invalid("URL must use http or https")
            expiresAt != null && !expiresAt.isAfter(now) -> ShortenResult.Invalid("expiresAt must be in the future")
            else -> allocate(originalUrl, now, expiresAt)
        }
    }

    private fun allocate(
        originalUrl: OriginalUrl,
        now: Instant,
        expiresAt: Instant?,
    ): ShortenResult {
        repeat(MAX_ATTEMPTS) {
            val link = ShortLink(codeGenerator.next(), originalUrl, now, expiresAt)
            if (store.insert(link)) {
                return ShortenResult.Created(link)
            }
        }
        return ShortenResult.Unavailable
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}

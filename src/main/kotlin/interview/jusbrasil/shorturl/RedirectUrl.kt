package interview.jusbrasil.shorturl

import org.springframework.stereotype.Component
import java.time.Clock

data class ClickRequest(
    val referrer: String,
    val userAgent: String,
    val ip: String,
)

sealed interface RedirectResult {
    data class Moved(
        val location: String,
    ) : RedirectResult

    data object NotFound : RedirectResult

    data object Gone : RedirectResult
}

@Component
class RedirectUrl(
    private val store: InMemoryShortLinkStore,
    private val clock: Clock,
) {
    fun execute(
        code: String,
        click: ClickRequest,
    ): RedirectResult {
        val link = store.find(code) ?: return RedirectResult.NotFound
        return moveOrExpire(link, click)
    }

    private fun moveOrExpire(
        link: ShortLink,
        click: ClickRequest,
    ): RedirectResult {
        val now = clock.instant()
        if (link.expiresAt != null && !link.expiresAt.isAfter(now)) {
            return RedirectResult.Gone
        }
        store.insertClick(
            NewClick(
                code = link.code,
                clickedAt = now,
                referrer = click.referrer,
                userAgent = click.userAgent,
                ip = click.ip,
            ),
        )
        return RedirectResult.Moved(link.originalUrl.value)
    }
}

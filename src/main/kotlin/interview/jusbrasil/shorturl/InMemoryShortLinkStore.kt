package interview.jusbrasil.shorturl

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

@Component
class InMemoryShortLinkStore {
    private val links = ConcurrentHashMap<String, ShortLink>()
    private val clicks = ConcurrentHashMap<String, CopyOnWriteArrayList<Click>>()
    private val clickIds = AtomicLong(1)

    fun insert(link: ShortLink): Boolean = links.putIfAbsent(link.code, link) == null

    fun find(code: String): ShortLink? = links[code]

    fun links(): List<ShortLink> = links.values.toList()

    fun insertClick(click: NewClick): Click {
        if (links[click.code] == null) {
            throw UnknownShortCodeException(click.code)
        }
        val stored =
            Click(
                id = clickIds.getAndIncrement(),
                code = click.code,
                clickedAt = click.clickedAt,
                referrer = click.referrer,
                userAgent = click.userAgent,
                ip = click.ip,
            )
        clicks.compute(click.code) { _, existing ->
            (existing ?: CopyOnWriteArrayList()).apply { add(stored) }
        }
        return stored
    }

    fun clicksOf(code: String): List<Click> = clicks[code]?.toList().orEmpty()
}

class UnknownShortCodeException(
    val code: String,
) : IllegalStateException("Short code does not exist")

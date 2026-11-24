package interview.jusbrasil.shorturl

import org.springframework.stereotype.Component

data class ListedLink(
    val link: ShortLink,
    val clickCount: Int,
)

@Component
class ListUrls(
    private val store: InMemoryShortLinkStore,
) {
    fun execute(): List<ListedLink> =
        store
            .links()
            .sortedWith(compareByDescending<ShortLink> { it.createdAt }.thenBy { it.code })
            .map { link -> ListedLink(link, store.clicksOf(link.code).size) }
}

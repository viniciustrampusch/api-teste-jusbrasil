package interview.jusbrasil.shorturl

import java.net.URI

@JvmInline
value class OriginalUrl private constructor(
    val value: String,
) {
    companion object {
        private val allowedSchemes = setOf("http", "https")

        fun parse(raw: String): OriginalUrl? {
            val uri = runCatching { URI(raw) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase()
            return if (scheme in allowedSchemes && !uri.host.isNullOrBlank()) {
                OriginalUrl(raw)
            } else {
                null
            }
        }
    }
}

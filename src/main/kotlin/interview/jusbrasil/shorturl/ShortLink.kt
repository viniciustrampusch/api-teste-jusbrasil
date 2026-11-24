package interview.jusbrasil.shorturl

import java.time.Instant

data class ShortLink(
    val code: String,
    val originalUrl: OriginalUrl,
    val createdAt: Instant,
    val expiresAt: Instant?,
) {
    init {
        require(ShortCodes.isValid(code)) { "Short code must be 7 alphanumeric characters" }
    }
}

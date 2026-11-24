package interview.jusbrasil.shorturl

import java.time.Instant

data class NewClick(
    val code: String,
    val clickedAt: Instant,
    val referrer: String,
    val userAgent: String,
    val ip: String,
)

data class Click(
    val id: Long,
    val code: String,
    val clickedAt: Instant,
    val referrer: String,
    val userAgent: String,
    val ip: String,
)

package interview.jusbrasil.shorturl

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Random

fun interface ShortCodeGenerator {
    fun next(): String
}

@Component
class RandomShortCodeGenerator(
    private val random: Random = SecureRandom(),
) : ShortCodeGenerator {
    override fun next(): String =
        buildString(ShortCodes.LENGTH) {
            repeat(ShortCodes.LENGTH) {
                append(ShortCodes.ALPHABET[random.nextInt(ShortCodes.ALPHABET.length)])
            }
        }
}

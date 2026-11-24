package interview.jusbrasil.shorturl

object ShortCodes {
    const val LENGTH = 7
    const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    private val pattern = Regex("^[0-9A-Za-z]{$LENGTH}$")

    fun isValid(code: String): Boolean = pattern.matches(code)
}

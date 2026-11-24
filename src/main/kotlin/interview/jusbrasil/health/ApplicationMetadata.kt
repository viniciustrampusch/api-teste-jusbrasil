package interview.jusbrasil.health

data class ApplicationMetadata(
    val name: String,
    val environment: String,
) {
    init {
        require(name.isNotBlank()) { "Application name must not be blank" }
        require(environment.isNotBlank()) { "Application environment must not be blank" }
    }
}

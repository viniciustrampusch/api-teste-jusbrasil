package interview.jusbrasil.health

import org.springframework.core.env.Environment

class ApplicationMetadataFactory(
    private val environment: Environment,
) {
    fun create(): ApplicationMetadata =
        ApplicationMetadata(
            name = environment.getRequiredProperty(APPLICATION_NAME_PROPERTY),
            environment = environment.getRequiredProperty(ENVIRONMENT_PROPERTY),
        )

    private companion object {
        const val APPLICATION_NAME_PROPERTY = "spring.application.name"
        const val ENVIRONMENT_PROPERTY = "app.environment"
    }
}

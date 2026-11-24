package interview.jusbrasil.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class ApplicationMetadataHealthIndicator(
    private val metadata: ApplicationMetadata,
) : HealthIndicator {
    override fun health(): Health =
        Health
            .up()
            .withDetail("application", metadata.name)
            .withDetail("environment", metadata.environment)
            .build()
}

package interview.jusbrasil.health

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class HealthConfiguration {
    @Bean
    fun applicationMetadata(environment: Environment): ApplicationMetadata = ApplicationMetadataFactory(environment).create()
}

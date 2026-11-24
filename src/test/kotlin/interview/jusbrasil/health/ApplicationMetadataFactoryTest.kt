package interview.jusbrasil.health

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment

class ApplicationMetadataFactoryTest {
    @Test
    fun `reads the application name and environment from configuration`() {
        val environment = mockk<Environment>()
        every { environment.getRequiredProperty("spring.application.name") } returns "engineering-baseline"
        every { environment.getRequiredProperty("app.environment") } returns "dev"
        val factory = ApplicationMetadataFactory(environment)

        val metadata = factory.create()

        assertEquals("engineering-baseline", metadata.name)
        assertEquals("dev", metadata.environment)
    }

    @Test
    fun `fails when a required property is missing`() {
        val environment = mockk<Environment>()
        every { environment.getRequiredProperty("spring.application.name") } throws
            IllegalStateException("required property is missing")
        val factory = ApplicationMetadataFactory(environment)

        val error =
            assertThrows(IllegalStateException::class.java) {
                factory.create()
            }

        assertTrue(error.message.orEmpty().contains("missing"))
    }
}

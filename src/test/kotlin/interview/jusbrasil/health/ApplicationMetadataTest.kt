package interview.jusbrasil.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.health.contributor.Status

class ApplicationMetadataTest {
    @Test
    fun `rejects a blank application name`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ApplicationMetadata(name = " ", environment = "dev")
            }

        assertTrue(error.message.orEmpty().contains("name"))
    }

    @Test
    fun `rejects a blank environment`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ApplicationMetadata(name = "engineering-baseline", environment = " ")
            }

        assertTrue(error.message.orEmpty().contains("environment"))
    }
}

class ApplicationMetadataHealthIndicatorTest {
    @Test
    fun `reports up and includes application identity`() {
        val indicator =
            ApplicationMetadataHealthIndicator(
                ApplicationMetadata(name = "engineering-baseline", environment = "dev"),
            )

        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals("engineering-baseline", health.details["application"])
        assertEquals("dev", health.details["environment"])
    }
}

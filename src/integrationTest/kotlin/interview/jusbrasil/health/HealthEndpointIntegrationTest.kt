package interview.jusbrasil.health

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["management.endpoint.health.show-details=always"])
@AutoConfigureMockMvc
class HealthEndpointIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `health endpoint reports up and includes application metadata`() {
        mockMvc
            .perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.applicationMetadata.status").value("UP"))
            .andExpect(jsonPath("$.components.applicationMetadata.details.application").value("engineering-baseline"))
            .andExpect(jsonPath("$.components.applicationMetadata.details.environment").value("local"))
    }

    @Test
    fun `liveness endpoint reports up`() {
        mockMvc
            .perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun `readiness endpoint reports up`() {
        mockMvc
            .perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}

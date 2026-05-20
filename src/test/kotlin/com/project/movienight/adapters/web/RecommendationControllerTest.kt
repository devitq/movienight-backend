package com.project.movienight.adapters.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `invalid content type should return bad request`() {
        mockMvc
            .get("/api/users/${UUID.randomUUID()}/recommendations") {
                param("contentType", "INVALID")
            }.andExpect {
                status { isBadRequest() }
            }
    }
}
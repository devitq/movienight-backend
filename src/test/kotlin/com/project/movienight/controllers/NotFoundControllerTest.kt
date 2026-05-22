package com.project.movienight.controllers

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class NotFoundControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `requesting non-existent path should return 404 JSON response`() {
        mockMvc
            .perform(get("/non-existent-path"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.traceId").exists())
    }

    @Test
    fun `requesting root path should return 404 JSON response`() {
        mockMvc
            .perform(get("/"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.traceId").exists())
    }
}

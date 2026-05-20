package com.project.movienight.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.adapters.web.dto.request.CreateUserRequest
import com.project.movienight.adapters.web.dto.request.EditUserRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `create user should return 201 CREATED`() {
        val request =
            CreateUserRequest(
                name = "John Doe",
                email = "john@example.com",
            )

        mockMvc
            .perform(
                post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `edit user should return updated user`() {
        val createRequest =
            CreateUserRequest(
                name = "Old Name",
                email = "edit@example.com",
            )

        val response =
            mockMvc
                .perform(
                    post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andReturn()

        val userId = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        val editRequest = EditUserRequest(name = "New Name")

        mockMvc
            .perform(
                patch("/api/users/$userId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(editRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.email").value("edit@example.com"))
    }

    @Test
    fun `delete user should return 204 NO CONTENT`() {
        val request =
            CreateUserRequest(
                name = "User To Delete",
                email = "delete@example.com",
            )

        val response =
            mockMvc
                .perform(
                    post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andReturn()

        val userId = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        mockMvc
            .perform(delete("/api/users/$userId"))
            .andExpect(status().isNoContent())
    }

    @Test
    fun `delete non-existent user should return 404`() {
        val nonExistentId = "123e4567-e89b-12d3-a456-426614174000"
        mockMvc
            .perform(delete("/api/users/$nonExistentId"))
            .andExpect(status().isNotFound())
    }
}

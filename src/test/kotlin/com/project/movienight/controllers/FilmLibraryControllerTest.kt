package com.project.movienight.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.CreateUserRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FilmLibraryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `add film to library should work`() {
        val userRequest = CreateUserRequest(
            name = "Film Adder",
            email = "adder@example.com",
        )
        val userResponse = mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)),
        ).andReturn()
        val userId = objectMapper.readTree(userResponse.response.contentAsString).get("id").asText()

        val filmRequest = CreateFilmRequest(
            title = "Library Film",
            description = "Film description",
        )
        val filmResponse = mockMvc.perform(
            post("/api/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filmRequest)),
        ).andReturn()
        val filmId = objectMapper.readTree(filmResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/users/$userId/library/films/$filmId"),
        )
            .andExpect(status().isCreated())
    }

    @Test
    fun `remove film from library should return 204`() {
        val userRequest = CreateUserRequest(
            name = "Remove Film",
            email = "remove@example.com",
        )
        val userResponse = mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)),
        ).andReturn()
        val userId = objectMapper.readTree(userResponse.response.contentAsString).get("id").asText()

        val filmRequest = CreateFilmRequest(
            title = "Film To Remove",
            description = "Will be removed",
        )
        val filmResponse = mockMvc.perform(
            post("/api/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filmRequest)),
        ).andReturn()
        val filmId = objectMapper.readTree(filmResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(post("/api/users/$userId/library/films/$filmId"))
        mockMvc.perform(delete("/api/users/$userId/library/films/$filmId"))
            .andExpect(status().isNoContent())
    }
}

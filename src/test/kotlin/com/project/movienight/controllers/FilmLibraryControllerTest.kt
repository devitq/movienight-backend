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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
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
        val userRequest =
            CreateUserRequest(
                name = "Film Adder",
                email = "adder@example.com",
            )
        val userResponse =
            mockMvc
                .perform(
                    post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)),
                ).andReturn()
        val userId = objectMapper.readTree(userResponse.response.contentAsString).get("id").asText()

        val filmRequest =
            CreateFilmRequest(
                title = "Library Film",
                description = "Film description",
            )
        val filmResponse =
            mockMvc
                .perform(
                    post("/api/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmRequest)),
                ).andReturn()
        val filmId = objectMapper.readTree(filmResponse.response.contentAsString).get("id").asText()

        mockMvc
            .perform(
                post("/api/users/$userId/library/films/$filmId"),
            ).andExpect(status().isCreated())
    }

    @Test
    fun `remove film from library should return 204`() {
        val userRequest =
            CreateUserRequest(
                name = "Remove Film",
                email = "remove@example.com",
            )
        val userResponse =
            mockMvc
                .perform(
                    post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)),
                ).andReturn()
        val userId = objectMapper.readTree(userResponse.response.contentAsString).get("id").asText()

        val filmRequest =
            CreateFilmRequest(
                title = "Film To Remove",
                description = "Will be removed",
            )
        val filmResponse =
            mockMvc
                .perform(
                    post("/api/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmRequest)),
                ).andReturn()
        val filmId = objectMapper.readTree(filmResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(post("/api/users/$userId/library/films/$filmId"))
        mockMvc
            .perform(delete("/api/users/$userId/library/films/$filmId"))
            .andExpect(status().isNoContent())
    }

    @Test
    fun `get available films should exclude film in user's library`() {
        val userRequest =
            CreateUserRequest(
                name = "Available Films User",
                email = "availablefilms@example.com",
            )
        val userResponse =
            mockMvc
                .perform(
                    post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)),
                ).andReturn()
        val userId = objectMapper.readTree(userResponse.response.contentAsString).get("id").asText()

        val film1Request =
            CreateFilmRequest(
                title = "Film In Library",
                description = "This will be in the library",
            )
        val film1Response =
            mockMvc
                .perform(
                    post("/api/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film1Request)),
                ).andReturn()
        val film1Id = objectMapper.readTree(film1Response.response.contentAsString).get("id").asText()

        val film2Request =
            CreateFilmRequest(
                title = "Film Not In Library",
                description = "This will not be in the library",
            )
        val film2Response =
            mockMvc
                .perform(
                    post("/api/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film2Request)),
                ).andReturn()
        val film2Id = objectMapper.readTree(film2Response.response.contentAsString).get("id").asText()

        mockMvc.perform(post("/api/users/$userId/library/films/$film1Id"))

        val result =
            mockMvc
                .perform(
                    get("/api/users/$userId/library/available-films"),
                ).andExpect(status().isOk)
                .andReturn()

        val responseBody = result.response.contentAsString
        val films = objectMapper.readTree(responseBody)
        val returnedIds = films.toList().map { it.get("id").asText() }
        assert(!returnedIds.contains(film1Id)) { "Film in library should not appear in available films" }
        assert(returnedIds.contains(film2Id)) { "Film not in library should appear in available films" }
    }

    @Test
    fun `get available films for user without library returns all films`() {
        val userRequest =
            CreateUserRequest(
                name = "No Library User",
                email = "nolibrary@example.com",
            )
        val userResponse =
            mockMvc
                .perform(
                    post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)),
                ).andReturn()
        val userId = objectMapper.readTree(userResponse.response.contentAsString).get("id").asText()

        val filmRequest =
            CreateFilmRequest(
                title = "Available Film",
                description = "Should appear in available films",
            )
        val filmResponse =
            mockMvc
                .perform(
                    post("/api/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmRequest)),
                ).andReturn()
        val filmId = objectMapper.readTree(filmResponse.response.contentAsString).get("id").asText()

        val result =
            mockMvc
                .perform(
                    get("/api/users/$userId/library/available-films"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$[*].id").isArray)
                .andReturn()

        val responseBody = result.response.contentAsString
        val films = objectMapper.readTree(responseBody)
        val returnedIds = films.toList().map { it.get("id").asText() }
        assert(returnedIds.contains(filmId)) { "Film should appear in available films when user has no library" }
    }
}

package com.project.movienight.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.EditFilmRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `create film should return 201 CREATED`() {
        val request = CreateFilmRequest(
            title = "The Matrix",
            description = "A computer hacker learns about the true nature of reality",
        )

        mockMvc.perform(
            post("/api/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("The Matrix"))
            .andExpect(jsonPath("$.description").value("A computer hacker learns about the true nature of reality"))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `edit film should return updated film`() {
        val createRequest = CreateFilmRequest(
            title = "Old Title",
            description = "Old Description"
        )

        val response = mockMvc.perform(
            post("/api/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val filmId = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        val editRequest = EditFilmRequest(
            title = "New Title",
            description = "New Description",
        )

        mockMvc.perform(
            patch("/api/films/$filmId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(editRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("New Title"))
            .andExpect(jsonPath("$.description").value("New Description"))
    }

    @Test
    fun `search film by title should return film`() {
        val request = CreateFilmRequest(
            title = "Inception",
            description = "Dream within a dream"
        )

        mockMvc.perform(
            post("/api/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )

        mockMvc.perform(
            get("/api/films/search")
                .param("title", "Inception")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Inception"))
            .andExpect(jsonPath("$.description").value("Dream within a dream"))
    }

    @Test
    fun `search film by non-existent title should return empty`() {
        mockMvc.perform(
            get("/api/films/search")
                .param("title", "NonExistentFilm12345")
        )
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }

    @Test
    fun `delete film should return 204 NO CONTENT`() {
        val request = CreateFilmRequest(
            title = "Film To Delete",
            description = "This film will be deleted"
        )

        val response = mockMvc.perform(
            post("/api/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val filmId = objectMapper.readTree(response.response.contentAsString).get("id").asText()

        mockMvc.perform(delete("/api/films/$filmId"))
            .andExpect(status().isNoContent())

        mockMvc.perform(get("/api/films/search").param("title", "Film To Delete"))
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }
}

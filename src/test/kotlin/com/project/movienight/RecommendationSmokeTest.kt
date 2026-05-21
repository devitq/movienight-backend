package com.project.movienight

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.CreateUserRequest
import com.project.movienight.adapters.web.dto.request.RateFilmRequest
import com.project.movienight.adapters.web.dto.request.UpsertUserPreferencesRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RecommendationSmokeTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        cleanDatabase()
    }

    @AfterEach
    fun cleanup() {
        cleanDatabase()
    }

    @Test
    fun `should create data and return a ranked recommendation`() {
        mockMvc
            .post("/api/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(CreateUserRequest(name = "Jane", email = "jane@example.com"))
            }.andExpect {
                status { isCreated() }
            }

        val userId =
            UUID.fromString(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?",
                    String::class.java,
                    "jane@example.com",
                ),
            )

        mockMvc
            .post("/api/films") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        CreateFilmRequest(
                            title = "Orbital Drift",
                            description = "A science-fiction rescue mission",
                            contentType = "FILM",
                            genres = listOf("SCI-FI", "THRILLER"),
                            directors = listOf("Nora Finch"),
                            imdbRating = 8.7,
                            platformRating = 9.0,
                            externalUrl = "https://example.com/orbital-drift",
                            jellyfinItemId = "orbital-drift-item",
                        ),
                    )
            }.andExpect {
                status { isCreated() }
            }

        mockMvc
            .post("/api/films") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        CreateFilmRequest(
                            title = "Small Town Summer",
                            description = "A grounded family drama",
                            contentType = "FILM",
                            genres = listOf("DRAMA"),
                            directors = listOf("Ava Reed"),
                            imdbRating = 7.1,
                            platformRating = 6.8,
                        ),
                    )
            }.andExpect {
                status { isCreated() }
            }

        val createdFilms = jdbcTemplate.queryForList("SELECT id, title FROM films ORDER BY title")
        val filmIdByTitle =
            createdFilms.associate { row ->
                row["title"].toString() to UUID.fromString(row["id"].toString())
            }
        val firstFilmId = filmIdByTitle.getValue("Orbital Drift")
        val secondFilmId = filmIdByTitle.getValue("Small Town Summer")

        mockMvc
            .put("/api/users/$userId/preferences") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        UpsertUserPreferencesRequest(
                            weightedGenres = mapOf("SCI-FI" to 5),
                            moods = listOf("focused"),
                            contentTypes = listOf("FILM"),
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.weightedGenres['SCI-FI']") { value(5) }
            }

        mockMvc
            .post("/api/users/$userId/ratings/films/$firstFilmId") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(RateFilmRequest(score = 10, note = "Great fit"))
            }.andExpect {
                status { isCreated() }
                jsonPath("$.score") { value(10) }
            }

        mockMvc
            .post("/api/users/$userId/library/films/$secondFilmId/viewed")
            .andExpect {
                status { isOk() }
                jsonPath("$.viewed") { value(true) }
            }

        mockMvc
            .get("/api/users/$userId/ratings")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].filmId") { value(firstFilmId.toString()) }
            }

        mockMvc
            .get("/api/users/$userId/recommendations") {
                param("contentType", "FILM")
                param("limit", "2")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].filmId") { value(firstFilmId.toString()) }
                jsonPath("$[0].film.id") { value(firstFilmId.toString()) }
                jsonPath("$[0].watchUrl") {
                    value("https://jellyfin.example.test/web/#/details?id=orbital-drift-item")
                }
                jsonPath("$[0].reasons[0]") { exists() }
            }

        mockMvc
            .post("/api/users/$userId/recommendations/$firstFilmId/accept")
            .andExpect {
                status { isOk() }
                jsonPath("$.filmId") { value(firstFilmId.toString()) }
                jsonPath("$.eventType") { value("ACCEPTED") }
            }

        mockMvc
            .post("/api/users/$userId/recommendations/$firstFilmId/reject")
            .andExpect {
                status { isOk() }
                jsonPath("$.filmId") { value(firstFilmId.toString()) }
                jsonPath("$.eventType") { value("REJECTED") }
            }

        mockMvc
            .get("/api/users/$userId/recommendations") {
                param("contentType", "FILM")
                param("libraryOnly", "true")
                param("limit", "2")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isEmpty() }
            }
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM recommendation_events")
        jdbcTemplate.execute("DELETE FROM film_ratings")
        jdbcTemplate.execute("DELETE FROM user_preferences")
        jdbcTemplate.execute("DELETE FROM favorites")
        jdbcTemplate.execute("DELETE FROM films")
        jdbcTemplate.execute("DELETE FROM users")
    }
}

package com.project.movienight

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.adapters.web.dto.request.CreateFilmRequest
import com.project.movienight.adapters.web.dto.request.CreateUserRequest
import com.project.movienight.adapters.web.dto.request.RateFilmRequest
import com.project.movienight.adapters.web.dto.request.RecommendationOnboardingRequest
import com.project.movienight.adapters.web.dto.request.UpdateUserRecommendationWeightsRequest
import com.project.movienight.adapters.web.dto.request.UpsertUserPreferencesRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
            .get("/api/users/$userId/recommendation-weights")
            .andExpect {
                status { isOk() }
                jsonPath("$.relevanceWeight") { value(0.55) }
                jsonPath("$.plotVectorWeight") { value(0.35) }
            }

        mockMvc
            .put("/api/users/$userId/recommendation-weights") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        UpdateUserRecommendationWeightsRequest(
                            relevanceWeight = 0.60,
                            qualityWeight = 0.10,
                            contextWeight = 0.15,
                            noveltyWeight = 0.10,
                            diversityWeight = 0.05,
                            genreVectorWeight = 0.30,
                            plotVectorWeight = 0.30,
                            moodVectorWeight = 0.20,
                            eraVectorWeight = 0.05,
                            peopleVectorWeight = 0.10,
                            contentTypeVectorWeight = 0.05,
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.relevanceWeight") { value(0.6) }
                jsonPath("$.genreVectorWeight") { value(0.3) }
            }

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

        val recommendedBreakdownCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM recommendation_events
                WHERE user_id = ?
                  AND film_id = ?
                  AND event_type = 'RECOMMENDED'
                  AND relevance_score IS NOT NULL
                  AND quality_score IS NOT NULL
                """.trimIndent(),
                Int::class.java,
                userId,
                firstFilmId,
            )
        assertTrue((recommendedBreakdownCount ?: 0) > 0)

        val weightsBeforeFeedback = findScoreWeights(userId)

        mockMvc
            .post("/api/users/$userId/recommendations/$firstFilmId/accept")
            .andExpect {
                status { isOk() }
                jsonPath("$.filmId") { value(firstFilmId.toString()) }
                jsonPath("$.eventType") { value("ACCEPTED") }
                jsonPath("$.relevanceScore") { exists() }
            }

        val weightsAfterAccept = findScoreWeights(userId)
        assertNotEquals(weightsBeforeFeedback, weightsAfterAccept)
        assertTrue(weightsAfterAccept.all { it in 0.05..0.75 })

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

    @Test
    fun `should complete recommendation onboarding`() {
        mockMvc
            .post("/api/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(CreateUserRequest(name = "Alex", email = "alex@example.com"))
            }.andExpect {
                status { isCreated() }
            }

        val userId =
            UUID.fromString(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?",
                    String::class.java,
                    "alex@example.com",
                ),
            )

        val likedFilmId = createFilm(title = "Neon Rescue", genres = listOf("SCI-FI"), imdbRating = 8.8)
        val dislikedFilmId = createFilm(title = "Quiet Village", genres = listOf("DRAMA"), imdbRating = 5.0)
        val libraryFilmId = createFilm(title = "Space Trial", genres = listOf("SCI-FI"), imdbRating = 7.8)
        val watchedFilmId = createFilm(title = "Old Mission", genres = listOf("THRILLER"), imdbRating = 8.1)

        mockMvc
            .post("/api/users/$userId/recommendation-onboarding") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        RecommendationOnboardingRequest(
                            weightedGenres = mapOf("SCI-FI" to 5, "THRILLER" to 3),
                            moods = listOf("focused", "tense"),
                            contentTypes = listOf("FILM"),
                            likedFilmIds = listOf(likedFilmId),
                            dislikedFilmIds = listOf(dislikedFilmId),
                            libraryFilmIds = listOf(libraryFilmId),
                            watchedFilmIds = listOf(watchedFilmId),
                            recommendationStyle = "DISCOVERY",
                        ),
                    )
            }.andExpect {
                status { isOk() }
                jsonPath("$.preferences.weightedGenres['SCI-FI']") { value(5) }
                jsonPath("$.weights.noveltyWeight") { value(0.25) }
                jsonPath("$.weights.diversityWeight") { value(0.25) }
                jsonPath("$.likedFilmsCount") { value(1) }
                jsonPath("$.dislikedFilmsCount") { value(1) }
                jsonPath("$.libraryFilmsCount") { value(1) }
                jsonPath("$.watchedFilmsCount") { value(1) }
            }

        assertDatabaseCount(
            """
            SELECT COUNT(*)
            FROM film_ratings
            WHERE user_id = ?
              AND film_id IN (?, ?)
            """.trimIndent(),
            userId,
            likedFilmId,
            dislikedFilmId,
        )
        assertDatabaseCount(
            """
            SELECT COUNT(*)
            FROM favorites
            WHERE user_id = ?
              AND film_id = ?
              AND is_viewed = TRUE
            """.trimIndent(),
            userId,
            watchedFilmId,
        )

        mockMvc
            .get("/api/users/$userId/recommendations") {
                param("contentType", "FILM")
                param("limit", "3")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].reasons[0]") { exists() }
            }
    }

    @Test
    fun `should rank films similar to highly rated choices above broad onboarding matches`() {
        mockMvc
            .post("/api/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(CreateUserRequest(name = "Harry", email = "harry@example.com"))
            }.andExpect {
                status { isCreated() }
            }

        val userId =
            UUID.fromString(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?",
                    String::class.java,
                    "harry@example.com",
                ),
            )

        val likedFirstFilmId =
            createFilm(
                title = "Wizard School Stone",
                description = "A young wizard discovers a magic school, spells, friendship, and a hidden dark force.",
                releaseYear = 2001,
                genres = listOf("Fantasy", "Adventure", "Family"),
                imdbRating = 8.0,
            )
        val likedSecondFilmId =
            createFilm(
                title = "Chamber of Magic",
                description = "Young friends return to a wizard school and uncover a secret chamber full of magical danger.",
                releaseYear = 2002,
                genres = listOf("Fantasy", "Adventure", "Family"),
                imdbRating = 8.1,
            )
        val magicCandidateId =
            createFilm(
                title = "Academy of Spells",
                description = "A group of friends learns spells at a magic academy while facing a dark wizard.",
                releaseYear = 2005,
                genres = listOf("Fantasy", "Adventure", "Family"),
                imdbRating = 7.0,
            )
        createFilm(
            title = "Highway Strike",
            description = "An elite agent chases criminals through explosions, heists, and street fights.",
            releaseYear = 2005,
            genres = listOf("Action"),
            imdbRating = 9.4,
        )

        mockMvc
            .put("/api/users/$userId/preferences") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        UpsertUserPreferencesRequest(
                            weightedGenres = mapOf("Action" to 5),
                            eras = listOf("2000s"),
                            contentTypes = listOf("FILM"),
                        ),
                    )
            }.andExpect {
                status { isOk() }
            }

        listOf(likedFirstFilmId, likedSecondFilmId).forEach { filmId ->
            mockMvc
                .post("/api/users/$userId/ratings/films/$filmId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(RateFilmRequest(score = 10, note = "Favorite"))
                }.andExpect {
                    status { isCreated() }
                }

            mockMvc
                .post("/api/users/$userId/library/films/$filmId/viewed")
                .andExpect {
                    status { isOk() }
                }
        }

        mockMvc
            .get("/api/users/$userId/recommendations") {
                param("contentType", "FILM")
                param("limit", "2")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].filmId") { value(magicCandidateId.toString()) }
                jsonPath("$[0].reasons[0]") { value("Similar to films you rated highly") }
            }
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM recommendation_events")
        jdbcTemplate.execute("DELETE FROM user_recommendation_weights")
        jdbcTemplate.execute("DELETE FROM film_ratings")
        jdbcTemplate.execute("DELETE FROM user_preferences")
        jdbcTemplate.execute("DELETE FROM favorites")
        jdbcTemplate.execute("DELETE FROM films")
        jdbcTemplate.execute("DELETE FROM users")
    }

    private fun findScoreWeights(userId: UUID): List<Double> =
        jdbcTemplate
            .queryForMap(
                """
                SELECT relevance_weight,
                       quality_weight,
                       context_weight,
                       novelty_weight,
                       diversity_weight
                FROM user_recommendation_weights
                WHERE user_id = ?
                """.trimIndent(),
                userId,
            ).let { row ->
                listOf(
                    row.getValue("RELEVANCE_WEIGHT"),
                    row.getValue("QUALITY_WEIGHT"),
                    row.getValue("CONTEXT_WEIGHT"),
                    row.getValue("NOVELTY_WEIGHT"),
                    row.getValue("DIVERSITY_WEIGHT"),
                ).map { (it as Number).toDouble() }
            }

    private fun createFilm(
        title: String,
        description: String = "$title description",
        releaseYear: Int? = null,
        genres: List<String>,
        imdbRating: Double,
    ): UUID {
        mockMvc
            .post("/api/films") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    objectMapper.writeValueAsString(
                        CreateFilmRequest(
                            title = title,
                            description = description,
                            contentType = "FILM",
                            releaseYear = releaseYear,
                            genres = genres,
                            imdbRating = imdbRating,
                        ),
                    )
            }.andExpect {
                status { isCreated() }
            }

        return UUID.fromString(
            jdbcTemplate.queryForObject(
                "SELECT id FROM films WHERE title = ?",
                String::class.java,
                title,
            ),
        )
    }

    private fun assertDatabaseCount(
        sql: String,
        vararg args: Any,
    ) {
        val count = jdbcTemplate.queryForObject(sql, Int::class.java, *args)
        assertTrue((count ?: 0) > 0)
    }
}

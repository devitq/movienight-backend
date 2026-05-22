package com.project.movienight.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "integrations.jellyfin.enabled=true",
        "integrations.jellyfin.plugin-token=test-token",
        "integrations.jellyfin.web-url=https://jellyfin.example.test",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class JellyfinPluginContractTest {
    private val jellyfinUserId = "11111111111111111111111111111111"
    private val dashedJellyfinUserId = "11111111-1111-1111-1111-111111111111"
    private val jellyfinItemId = "22222222222222222222222222222222"
    private val dashedJellyfinItemId = "22222222-2222-2222-2222-222222222222"

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `plugin sync payload creates mapped user and film`() {
        postSyncPayload()

        val syncedRecommendationTitlePath = "$[?(@.jellyfinItemId == '$jellyfinItemId')].title"
        val syncedRecommendationWatchUrlPath = "$[?(@.jellyfinItemId == '$jellyfinItemId')].watchUrl"
        val expectedWatchUrl = "https://jellyfin.example.test/web/#/details?id=$jellyfinItemId"

        mockMvc
            .perform(
                get("/api/integrations/jellyfin/users/$dashedJellyfinUserId/recommendations")
                    .header("X-MovieNight-Plugin-Token", "test-token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[*].jellyfinItemId").value(hasItem(jellyfinItemId)))
            .andExpect(jsonPath(syncedRecommendationTitlePath).value(hasItem("Jellyfin Contract Film")))
            .andExpect(jsonPath(syncedRecommendationWatchUrlPath).value(hasItem(expectedWatchUrl)))

        mockMvc
            .perform(
                post("/api/integrations/jellyfin/users/$dashedJellyfinUserId/ratings/items/$dashedJellyfinItemId")
                    .header("X-MovieNight-Plugin-Token", "test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"score":8,"note":"From Jellyfin UI"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.score").value(8))

        val viewedPath =
            "/api/integrations/jellyfin/users/$dashedJellyfinUserId/library/items/" +
                "$dashedJellyfinItemId/viewed"

        mockMvc
            .perform(
                post(viewedPath)
                    .header("X-MovieNight-Plugin-Token", "test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"watchedAt":"2026-05-22T10:15:30Z"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.viewed").value(true))

        mockMvc
            .perform(
                get("/api/integrations/jellyfin/sync-state")
                    .header("X-MovieNight-Plugin-Token", "test-token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].syncedItemCount").value(1))
    }

    @Test
    fun `plugin token is required when configured`() {
        mockMvc
            .perform(
                post("/api/integrations/jellyfin/sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(syncPayload())),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `plugin onboarding can create user before first sync`() {
        mockMvc
            .perform(
                post("/api/integrations/jellyfin/users/$dashedJellyfinUserId/recommendation-onboarding")
                    .header("X-MovieNight-Plugin-Token", "test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"weightedGenres":{"Drama":5},"contentTypes":["FILM"]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").exists())
    }

    private fun postSyncPayload() {
        mockMvc
            .perform(
                post("/api/integrations/jellyfin/sync")
                    .header("X-MovieNight-Plugin-Token", "test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(syncPayload())),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.syncedUsers").value(1))
            .andExpect(jsonPath("$.syncedItems").value(1))
    }

    private fun syncPayload(): Map<String, Any?> =
        mapOf(
            "users" to
                listOf(
                    mapOf(
                        "jellyfinUserId" to jellyfinUserId,
                        "name" to "Jellyfin User",
                    ),
                ),
            "items" to
                listOf(
                    mapOf(
                        "jellyfinItemId" to jellyfinItemId,
                        "title" to "Jellyfin Contract Film",
                        "description" to "Synced from plugin payload",
                        "year" to 2026,
                        "genres" to listOf("Drama"),
                        "imdbId" to "tt1234567",
                        "userStates" to
                            listOf(
                                mapOf(
                                    "jellyfinUserId" to jellyfinUserId,
                                    "isViewed" to false,
                                    "playCount" to 0,
                                ),
                            ),
                    ),
                ),
        )
}

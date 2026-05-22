package com.project.movienight.adapters.jellyfin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.project.movienight.application.ports.output.JellyfinCatalogPort
import com.project.movienight.application.ports.output.JellyfinLibraryItemSnapshot
import com.project.movienight.application.ports.output.JellyfinRemoteUser
import com.project.movienight.config.JellyfinIntegrationProperties
import com.project.movienight.domain.model.ContentType
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class JellyfinApiClient(
    private val properties: JellyfinIntegrationProperties,
    private val objectMapper: ObjectMapper,
) : JellyfinCatalogPort {
    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(properties.requestTimeoutMs))
            .build()

    override fun fetchUsers(): List<JellyfinRemoteUser> =
        request("Users")
            .asItems()
            .mapNotNull { node ->
                val id = node.fieldText("Id") ?: return@mapNotNull null
                JellyfinRemoteUser(id = id, name = node.fieldText("Name") ?: id)
            }

    override fun fetchLibraryItems(userId: String): List<JellyfinLibraryItemSnapshot> =
        @Suppress("MaxLineLength")
        request(
            "Users/$userId/Items?Recursive=true&IncludeItemTypes=Movie,Series,Episode&Fields=Genres,People,ProviderIds,Overview,ProductionYear,CommunityRating,OfficialRating,ParentId,UserData",
        ).asItems().mapNotNull { node ->
            val itemId = node.fieldText("Id") ?: return@mapNotNull null
            val providerIds = node["ProviderIds"]
            val imdbId = providerIds?.fieldText("Imdb")
            val people = node["People"]
            val cast = people?.peopleByType("Actor", "GuestStar") ?: emptyList()
            val directors = people?.peopleByType("Director") ?: emptyList()
            JellyfinLibraryItemSnapshot(
                jellyfinItemId = itemId,
                title = node.fieldText("Name") ?: itemId,
                description = node.fieldText("Overview") ?: "",
                contentType = mapContentType(node.fieldText("Type")),
                releaseYear = node["ProductionYear"]?.takeUnless { it.isNull }?.asInt(),
                genres = node["Genres"]?.textList() ?: emptyList(),
                cast = cast,
                directors = directors,
                platformRating = node["CommunityRating"]?.takeUnless { it.isNull }?.asDouble(),
                imdbRating = null,
                externalUrl = imdbId?.let { "https://www.imdb.com/title/$it/" },
                jellyfinLibraryId = node.fieldText("ParentId"),
                isPlayed =
                    node["UserData"]?.booleanField("Played") ?: node["UserData"]?.booleanField("IsPlayed") ?: false,
            )
        }

    private fun request(path: String): JsonNode {
        val uri = URI.create("${properties.baseUrl.trimEnd('/')}/$path")
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofMillis(properties.requestTimeoutMs))
                .header("Accept", "application/json")
                .header("X-Emby-Token", properties.apiKey)
                .GET()
                .build()

        val response =
            try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (
                @Suppress("TooGenericExceptionCaught") exception: Exception,
            ) {
                throw IllegalStateException("Failed to call Jellyfin at $uri", exception)
            }

        check(response.statusCode() in 200..299) {
            "Jellyfin request failed with status ${response.statusCode()} for $uri"
        }

        return objectMapper.readTree(response.body())
    }

    private fun JsonNode.asItems(): List<JsonNode> =
        when {
            isArray -> map { it }
            has("Items") && this["Items"].isArray -> this["Items"].map { it }
            else -> emptyList()
        }

    private fun JsonNode.fieldText(name: String): String? =
        get(name)?.takeUnless { it.isNull }?.asText()?.takeIf { it.isNotBlank() }

    private fun JsonNode.booleanField(name: String): Boolean? = get(name)?.takeUnless { it.isNull }?.asBoolean()

    private fun JsonNode.textList(): List<String> =
        takeIf { it.isArray }?.mapNotNull { item ->
            item.takeUnless { it.isNull }?.asText()?.takeIf { text -> text.isNotBlank() }
        }
            ?: emptyList()

    private fun JsonNode.peopleByType(vararg types: String): List<String> {
        if (!isArray) return emptyList()
        return mapNotNull { person ->
            val type = person.fieldText("Type") ?: return@mapNotNull null
            if (types.any { it.equals(type, ignoreCase = true) }) person.fieldText("Name") else null
        }
    }

    private fun mapContentType(value: String?): ContentType =
        when (value?.lowercase()) {
            "movie" -> ContentType.FILM
            "series" -> ContentType.SERIES
            "episode" -> ContentType.EPISODE
            else -> ContentType.OTHER
        }
}

package com.project.movienight.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "services.film")
data class FilmServiceProperties(
    val blockedPatterns: List<String> = listOf(),
) {
    val normalized: Set<String> =
        blockedPatterns
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

    fun isBlocked(fieldValue: String): Boolean {
        val normalizedFieldValue = fieldValue.lowercase()

        return normalized.any { pattern -> normalizedFieldValue.contains(pattern) }
    }
}

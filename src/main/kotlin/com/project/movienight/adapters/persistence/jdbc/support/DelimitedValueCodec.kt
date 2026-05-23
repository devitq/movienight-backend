package com.project.movienight.adapters.persistence.jdbc.support

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DelimitedValueCodec {
    fun encodeList(values: List<String>): String = values.joinToString("|") { encode(it) }

    fun decodeList(value: String?): List<String> =
        value
            ?.takeIf { it.isNotBlank() }
            ?.split("|")
            ?.map { decode(it) }
            ?: emptyList()

    fun encodeWeightedMap(values: Map<String, Int>): String =
        values.entries.joinToString("|") { entry -> "${encode(entry.key)}:${entry.value}" }

    fun decodeWeightedMap(value: String?): Map<String, Int> {
        if (value.isNullOrBlank()) return emptyMap()

        return value
            .split("|")
            .mapNotNull { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size != 2) return@mapNotNull null

                val key = decode(parts[0])
                val weight = parts[1].toIntOrNull() ?: return@mapNotNull null
                key to weight
            }.toMap()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)
}

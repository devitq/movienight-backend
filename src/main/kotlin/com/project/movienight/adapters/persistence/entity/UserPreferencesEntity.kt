package com.project.movienight.adapters.persistence.entity

import com.project.movienight.adapters.persistence.jdbc.support.DelimitedValueCodec
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.UserPreferences
import java.util.UUID

data class UserPreferencesEntity(
    val userId: UUID,
    val weightedGenres: String,
    val plotTypes: String,
    val eras: String,
    val castAndDirectors: String,
    val moods: String,
    val contentTypes: String,
)

fun UserPreferencesEntity.toDomain(): UserPreferences =
    UserPreferences(
        userId = userId,
        weightedGenres = DelimitedValueCodec.decodeWeightedMap(weightedGenres),
        plotTypes = DelimitedValueCodec.decodeList(plotTypes),
        eras = DelimitedValueCodec.decodeList(eras),
        castAndDirectors = DelimitedValueCodec.decodeList(castAndDirectors),
        moods = DelimitedValueCodec.decodeList(moods),
        contentTypes =
            DelimitedValueCodec.decodeList(contentTypes).mapNotNull { value ->
                runCatching { ContentType.valueOf(value) }.getOrNull()
            },
    )

fun UserPreferences.toEntity(): UserPreferencesEntity =
    UserPreferencesEntity(
        userId = userId,
        weightedGenres = DelimitedValueCodec.encodeWeightedMap(weightedGenres),
        plotTypes = DelimitedValueCodec.encodeList(plotTypes),
        eras = DelimitedValueCodec.encodeList(eras),
        castAndDirectors = DelimitedValueCodec.encodeList(castAndDirectors),
        moods = DelimitedValueCodec.encodeList(moods),
        contentTypes = DelimitedValueCodec.encodeList(contentTypes.map { it.name }),
    )

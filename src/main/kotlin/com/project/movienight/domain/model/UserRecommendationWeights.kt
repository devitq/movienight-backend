package com.project.movienight.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class UserRecommendationWeights(
    val userId: UUID,
    val relevanceWeight: Double = DEFAULT_RELEVANCE_WEIGHT,
    val qualityWeight: Double = DEFAULT_QUALITY_WEIGHT,
    val contextWeight: Double = DEFAULT_CONTEXT_WEIGHT,
    val noveltyWeight: Double = DEFAULT_NOVELTY_WEIGHT,
    val diversityWeight: Double = DEFAULT_DIVERSITY_WEIGHT,
    val genreVectorWeight: Double = DEFAULT_GENRE_VECTOR_WEIGHT,
    val plotVectorWeight: Double = DEFAULT_PLOT_VECTOR_WEIGHT,
    val moodVectorWeight: Double = DEFAULT_MOOD_VECTOR_WEIGHT,
    val eraVectorWeight: Double = DEFAULT_ERA_VECTOR_WEIGHT,
    val peopleVectorWeight: Double = DEFAULT_PEOPLE_VECTOR_WEIGHT,
    val contentTypeVectorWeight: Double = DEFAULT_CONTENT_TYPE_VECTOR_WEIGHT,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun normalized(updatedAt: LocalDateTime = this.updatedAt): UserRecommendationWeights {
        val scoreWeights =
            normalizeBounded(
                values =
                    listOf(
                        relevanceWeight,
                        qualityWeight,
                        contextWeight,
                        noveltyWeight,
                        diversityWeight,
                    ),
                defaults = DEFAULT_SCORE_WEIGHTS,
                min = MIN_SCORE_WEIGHT,
                max = MAX_SCORE_WEIGHT,
            )
        val vectorWeights =
            normalizeBounded(
                values =
                    listOf(
                        genreVectorWeight,
                        plotVectorWeight,
                        moodVectorWeight,
                        eraVectorWeight,
                        peopleVectorWeight,
                        contentTypeVectorWeight,
                    ),
                defaults = DEFAULT_VECTOR_WEIGHTS,
                min = MIN_VECTOR_WEIGHT,
                max = MAX_VECTOR_WEIGHT,
            )

        return copy(
            relevanceWeight = scoreWeights[0],
            qualityWeight = scoreWeights[1],
            contextWeight = scoreWeights[2],
            noveltyWeight = scoreWeights[3],
            diversityWeight = scoreWeights[4],
            genreVectorWeight = vectorWeights[0],
            plotVectorWeight = vectorWeights[1],
            moodVectorWeight = vectorWeights[2],
            eraVectorWeight = vectorWeights[3],
            peopleVectorWeight = vectorWeights[4],
            contentTypeVectorWeight = vectorWeights[5],
            updatedAt = updatedAt,
        )
    }

    companion object {
        const val DEFAULT_RELEVANCE_WEIGHT = 0.55
        const val DEFAULT_QUALITY_WEIGHT = 0.15
        const val DEFAULT_CONTEXT_WEIGHT = 0.10
        const val DEFAULT_NOVELTY_WEIGHT = 0.10
        const val DEFAULT_DIVERSITY_WEIGHT = 0.10

        const val DEFAULT_GENRE_VECTOR_WEIGHT = 0.25
        const val DEFAULT_PLOT_VECTOR_WEIGHT = 0.35
        const val DEFAULT_MOOD_VECTOR_WEIGHT = 0.15
        const val DEFAULT_ERA_VECTOR_WEIGHT = 0.10
        const val DEFAULT_PEOPLE_VECTOR_WEIGHT = 0.10
        const val DEFAULT_CONTENT_TYPE_VECTOR_WEIGHT = 0.05

        const val MIN_SCORE_WEIGHT = 0.05
        const val MAX_SCORE_WEIGHT = 0.75
        const val MIN_VECTOR_WEIGHT = 0.03
        const val MAX_VECTOR_WEIGHT = 0.60

        private val DEFAULT_SCORE_WEIGHTS =
            listOf(
                DEFAULT_RELEVANCE_WEIGHT,
                DEFAULT_QUALITY_WEIGHT,
                DEFAULT_CONTEXT_WEIGHT,
                DEFAULT_NOVELTY_WEIGHT,
                DEFAULT_DIVERSITY_WEIGHT,
            )
        private val DEFAULT_VECTOR_WEIGHTS =
            listOf(
                DEFAULT_GENRE_VECTOR_WEIGHT,
                DEFAULT_PLOT_VECTOR_WEIGHT,
                DEFAULT_MOOD_VECTOR_WEIGHT,
                DEFAULT_ERA_VECTOR_WEIGHT,
                DEFAULT_PEOPLE_VECTOR_WEIGHT,
                DEFAULT_CONTENT_TYPE_VECTOR_WEIGHT,
            )

        fun defaultFor(userId: UUID): UserRecommendationWeights = UserRecommendationWeights(userId = userId)

        fun forStyle(
            userId: UUID,
            style: RecommendationStyle,
        ): UserRecommendationWeights =
            when (style) {
                RecommendationStyle.BALANCED -> {
                    defaultFor(userId)
                }

                RecommendationStyle.QUALITY_FIRST -> {
                    UserRecommendationWeights(
                        userId = userId,
                        relevanceWeight = 0.40,
                        qualityWeight = 0.35,
                        contextWeight = 0.10,
                        noveltyWeight = 0.05,
                        diversityWeight = 0.10,
                    )
                }

                RecommendationStyle.MOOD_FIRST -> {
                    UserRecommendationWeights(
                        userId = userId,
                        relevanceWeight = 0.45,
                        qualityWeight = 0.10,
                        contextWeight = 0.25,
                        noveltyWeight = 0.10,
                        diversityWeight = 0.10,
                        moodVectorWeight = 0.30,
                    )
                }

                RecommendationStyle.DISCOVERY -> {
                    UserRecommendationWeights(
                        userId = userId,
                        relevanceWeight = 0.30,
                        qualityWeight = 0.10,
                        contextWeight = 0.10,
                        noveltyWeight = 0.25,
                        diversityWeight = 0.25,
                    )
                }

                RecommendationStyle.SIMILAR_TO_FAVORITES -> {
                    UserRecommendationWeights(
                        userId = userId,
                        relevanceWeight = 0.70,
                        qualityWeight = 0.10,
                        contextWeight = 0.10,
                        noveltyWeight = 0.05,
                        diversityWeight = 0.05,
                        genreVectorWeight = 0.30,
                        plotVectorWeight = 0.40,
                        peopleVectorWeight = 0.15,
                    )
                }
            }.normalized()

        private fun normalizeBounded(
            values: List<Double>,
            defaults: List<Double>,
            min: Double,
            max: Double,
        ): List<Double> {
            val sanitized = values.map { value -> if (value.isFinite() && value > 0.0) value else 0.0 }
            val source = sanitized.takeIf { it.sum() > 0.0 } ?: defaults
            val normalized = source.map { it / source.sum() }
            return projectToBounds(normalized, min, max)
        }

        private fun projectToBounds(
            values: List<Double>,
            min: Double,
            max: Double,
        ): List<Double> {
            val result = values.map { it.coerceIn(min, max) }.toMutableList()
            var iterations = 0
            var adjusting = true

            while (iterations < values.size * 2 && adjusting) {
                iterations += 1
                val diff = 1.0 - result.sum()
                if (kotlin.math.abs(diff) <= NORMALIZATION_EPSILON) {
                    adjusting = false
                } else {
                    adjusting = redistribute(result, diff, min, max)
                }
            }

            return result
        }

        private fun redistribute(
            result: MutableList<Double>,
            diff: Double,
            min: Double,
            max: Double,
        ): Boolean =
            if (diff > 0.0) {
                val candidates = result.indices.filter { result[it] < max }
                val capacity = candidates.sumOf { max - result[it] }
                if (capacity > 0.0) {
                    candidates.forEach { index ->
                        val increment = diff * ((max - result[index]) / capacity)
                        result[index] = (result[index] + increment).coerceAtMost(max)
                    }
                    true
                } else {
                    false
                }
            } else {
                val candidates = result.indices.filter { result[it] > min }
                val capacity = candidates.sumOf { result[it] - min }
                if (capacity > 0.0) {
                    candidates.forEach { index ->
                        val decrement = -diff * ((result[index] - min) / capacity)
                        result[index] = (result[index] - decrement).coerceAtLeast(min)
                    }
                    true
                } else {
                    false
                }
            }

        private const val NORMALIZATION_EPSILON = 0.0000001
    }
}

package com.project.movienight.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class UserRecommendationWeightsTest {
    @Test
    fun `should keep default weights normalized`() {
        val weights = UserRecommendationWeights.defaultFor(UUID.randomUUID()).normalized()

        assertEquals(1.0, weights.scoreWeightSum(), EPSILON)
        assertEquals(1.0, weights.vectorWeightSum(), EPSILON)
        assertEquals(0.55, weights.relevanceWeight, EPSILON)
        assertEquals(0.35, weights.plotVectorWeight, EPSILON)
    }

    @Test
    fun `should normalize and bound invalid weights`() {
        val weights =
            UserRecommendationWeights(
                userId = UUID.randomUUID(),
                relevanceWeight = 100.0,
                qualityWeight = -5.0,
                contextWeight = 0.0,
                noveltyWeight = 0.0,
                diversityWeight = 0.0,
                genreVectorWeight = 100.0,
                plotVectorWeight = 0.0,
                moodVectorWeight = 0.0,
                eraVectorWeight = 0.0,
                peopleVectorWeight = 0.0,
                contentTypeVectorWeight = 0.0,
            ).normalized()

        assertEquals(1.0, weights.scoreWeightSum(), EPSILON)
        assertEquals(1.0, weights.vectorWeightSum(), EPSILON)
        assertTrue(
            listOf(
                weights.relevanceWeight,
                weights.qualityWeight,
                weights.contextWeight,
                weights.noveltyWeight,
                weights.diversityWeight,
            ).all { it in UserRecommendationWeights.MIN_SCORE_WEIGHT..UserRecommendationWeights.MAX_SCORE_WEIGHT },
        )
        assertTrue(
            listOf(
                weights.genreVectorWeight,
                weights.plotVectorWeight,
                weights.moodVectorWeight,
                weights.eraVectorWeight,
                weights.peopleVectorWeight,
                weights.contentTypeVectorWeight,
            ).all { it in UserRecommendationWeights.MIN_VECTOR_WEIGHT..UserRecommendationWeights.MAX_VECTOR_WEIGHT },
        )
    }

    private fun UserRecommendationWeights.scoreWeightSum(): Double =
        relevanceWeight + qualityWeight + contextWeight + noveltyWeight + diversityWeight

    private fun UserRecommendationWeights.vectorWeightSum(): Double =
        genreVectorWeight +
            plotVectorWeight +
            moodVectorWeight +
            eraVectorWeight +
            peopleVectorWeight +
            contentTypeVectorWeight

    private companion object {
        private const val EPSILON = 0.000001
    }
}

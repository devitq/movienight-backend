package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.JellyfinSyncSummary
import com.project.movienight.domain.model.RecommendationEventType

interface BusinessMetricsPort {
    fun recordFilmCreated()

    fun recordFilmEdited()

    fun recordFilmDeleted()

    fun recordFilmBlocked()

    fun recordRecommendationRequest()

    fun recordRecommendationWeightsUpdated(eventType: RecommendationEventType)

    fun recordRatingSubmitted()

    fun recordLibraryEvent()

    fun recordJellyfinSync(summary: JellyfinSyncSummary)

    fun recordJellyfinSyncFailure()

    fun recordJellyfinUnmappedUser()

    fun recordBackendWriteFailure()
}

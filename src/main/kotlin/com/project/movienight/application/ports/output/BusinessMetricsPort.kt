package com.project.movienight.application.ports.output

import com.project.movienight.domain.model.JellyfinSyncSummary

interface BusinessMetricsPort {
    fun recordRecommendationRequest()

    fun recordRatingSubmitted()

    fun recordLibraryEvent()

    fun recordJellyfinSync(summary: JellyfinSyncSummary)

    fun recordJellyfinSyncFailure()

    fun recordJellyfinUnmappedUser()

    fun recordBackendWriteFailure()
}
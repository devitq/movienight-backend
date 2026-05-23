package com.project.movienight.adapters.metrics

import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.domain.model.JellyfinSyncSummary
import com.project.movienight.domain.model.RecommendationEventType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class BusinessMetricsService(
    private val meterRegistry: MeterRegistry,
) : BusinessMetricsPort {
    private val recommendationRequests: Counter = meterRegistry.counter("business_recommendation_requests_total")
    private val filmsCreated: Counter = meterRegistry.counter("business_films_created_total")
    private val filmsEdited: Counter = meterRegistry.counter("business_films_edited_total")
    private val filmsDeleted: Counter = meterRegistry.counter("business_films_deleted_total")
    private val filmsBlocked: Counter = meterRegistry.counter("business_films_blocked_total")
    private val ratingsSubmitted: Counter = meterRegistry.counter("business_ratings_submitted_total")
    private val libraryEvents: Counter = meterRegistry.counter("business_library_events_total")
    private val jellyfinSyncRuns: Counter = meterRegistry.counter("business_jellyfin_sync_runs_total")
    private val jellyfinSyncedUsers: Counter = meterRegistry.counter("business_jellyfin_synced_users_total")
    private val jellyfinSkippedUsers: Counter = meterRegistry.counter("business_jellyfin_skipped_users_total")
    private val jellyfinSyncedItems: Counter = meterRegistry.counter("business_jellyfin_synced_items_total")
    private val jellyfinSyncDuration: Timer =
        Timer
            .builder("business_jellyfin_sync_duration")
            .publishPercentileHistogram()
            .register(meterRegistry)
    private val jellyfinSyncFailures: Counter = meterRegistry.counter("business_jellyfin_sync_failures_total")
    private val jellyfinUnmappedUsersGaugeValue = AtomicInteger(0)

    init {
        meterRegistry.gauge("business_jellyfin_unmapped_users", jellyfinUnmappedUsersGaugeValue)
    }

    private val backendWriteFailures: Counter = meterRegistry.counter("business_jellyfin_backend_write_failures_total")

    override fun recordFilmCreated() {
        filmsCreated.increment()
    }

    override fun recordFilmEdited() {
        filmsEdited.increment()
    }

    override fun recordFilmDeleted() {
        filmsDeleted.increment()
    }

    override fun recordFilmBlocked() {
        filmsBlocked.increment()
    }

    override fun recordRecommendationRequest() {
        recommendationRequests.increment()
    }

    override fun recordRecommendationWeightsUpdated(eventType: RecommendationEventType) {
        Counter
            .builder("recommendation_weights_updated_total")
            .tag("eventType", eventType.name)
            .register(meterRegistry)
            .increment()
    }

    override fun recordRatingSubmitted() {
        ratingsSubmitted.increment()
    }

    override fun recordLibraryEvent() {
        libraryEvents.increment()
    }

    override fun recordJellyfinSync(summary: JellyfinSyncSummary) {
        jellyfinSyncRuns.increment()
        jellyfinSyncedUsers.increment(summary.syncedUsers.toDouble())
        jellyfinSkippedUsers.increment(summary.skippedUsers.toDouble())
        jellyfinSyncedItems.increment(summary.syncedItems.toDouble())
        jellyfinSyncDuration.record(summary.durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    override fun recordJellyfinSyncFailure() {
        jellyfinSyncFailures.increment()
    }

    override fun recordJellyfinUnmappedUser() {
        jellyfinUnmappedUsersGaugeValue.incrementAndGet()
    }

    override fun recordBackendWriteFailure() {
        backendWriteFailures.increment()
    }
}

package com.project.movienight.application.services

import com.project.movienight.application.ports.input.PushJellyfinCatalogCommand
import com.project.movienight.application.ports.input.PushedJellyfinItem
import com.project.movienight.application.ports.input.PushedJellyfinUserState
import com.project.movienight.application.ports.output.BusinessMetricsPort
import com.project.movienight.application.ports.output.FilmLibraryEntryRepositoryPort
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.application.ports.output.IdGenerator
import com.project.movienight.application.ports.output.JellyfinSyncStateRepositoryPort
import com.project.movienight.application.ports.output.UserRepositoryPort
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.FilmLibraryEntry
import com.project.movienight.domain.model.JellyfinSyncState
import com.project.movienight.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class JellyfinSyncServiceTest {
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var filmRepository: FilmRepositoryPort
    private lateinit var filmLibraryEntryRepository: FilmLibraryEntryRepositoryPort
    private lateinit var syncStateRepository: JellyfinSyncStateRepositoryPort
    private lateinit var idGenerator: IdGenerator
    private lateinit var businessMetricsService: BusinessMetricsPort
    private lateinit var service: JellyfinSyncService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        filmRepository = mockk()
        filmLibraryEntryRepository = mockk()
        syncStateRepository = mockk()
        idGenerator = mockk()
        businessMetricsService = mockk(relaxed = true)
        service =
            JellyfinSyncService(
                userRepository,
                filmRepository,
                filmLibraryEntryRepository,
                syncStateRepository,
                idGenerator,
                businessMetricsService,
            )
    }

    @Test
    fun `sync should upsert pushed Jellyfin item and mark viewed state`() {
        val userId = UUID.randomUUID()
        val filmId = UUID.randomUUID()
        val entryId = UUID.randomUUID()
        val watchedAt = OffsetDateTime.parse("2026-05-22T12:00:00Z")
        val savedFilmSlot = slot<Film>()
        val savedEntrySlot = slot<FilmLibraryEntry>()
        val savedStateSlot = slot<JellyfinSyncState>()

        every { userRepository.findAll() } returns
            listOf(User(userId, "Jellyfin User", "jellyfin@example.com", jellyfinUserId = "jf-user"))
        every { filmRepository.findByJellyfinItemId("jf-item") } returns null
        every { idGenerator.generateId() } returnsMany listOf(filmId, entryId)
        every { filmRepository.save(capture(savedFilmSlot)) } answers { savedFilmSlot.captured }
        every { filmLibraryEntryRepository.findByUserIdAndFilmId(userId, filmId) } returns null
        every { filmLibraryEntryRepository.save(capture(savedEntrySlot)) } answers { savedEntrySlot.captured }
        every { syncStateRepository.save(capture(savedStateSlot)) } answers { savedStateSlot.captured }

        val summary =
            service.sync(
                PushJellyfinCatalogCommand(
                    items =
                        listOf(
                            PushedJellyfinItem(
                                jellyfinItemId = "jf-item",
                                jellyfinLibraryId = "library-1",
                                title = "Pushed Movie",
                                description = "From Jellyfin",
                                contentType = ContentType.FILM,
                                releaseYear = 2026,
                                genres = listOf("Drama"),
                                cast = listOf("Actor One"),
                                directors = listOf("Director One"),
                                platformRating = 8.1,
                                imdbRating = 7.9,
                                externalUrl = null,
                                imdbId = "tt1234567",
                                userStates =
                                    listOf(
                                        PushedJellyfinUserState(
                                            jellyfinUserId = "jf-user",
                                            isViewed = true,
                                            lastPlayedAt = watchedAt,
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(1, summary.syncedUsers)
        assertEquals(0, summary.skippedUsers)
        assertEquals(1, summary.syncedItems)
        assertEquals("Pushed Movie", savedFilmSlot.captured.title)
        assertEquals("library-1", savedFilmSlot.captured.jellyfinLibraryId)
        assertEquals("https://www.imdb.com/title/tt1234567/", savedFilmSlot.captured.externalUrl)
        assertEquals(userId, savedEntrySlot.captured.userId)
        assertEquals(filmId, savedEntrySlot.captured.filmId)
        assertEquals(watchedAt.toLocalDateTime(), savedEntrySlot.captured.watchedAt)
        assertEquals(1, savedStateSlot.captured.syncedItemCount)

        verify(exactly = 1) { businessMetricsService.recordJellyfinSync(summary) }
    }
}

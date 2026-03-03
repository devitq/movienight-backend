package com.project.movienight.adapters.persistence.inmemory

import com.project.movienight.application.ports.output.FilmLibraryRepositoryPort
import com.project.movienight.domain.model.FilmLibrary
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class FilmLibraryRepositoryAdapter : FilmLibraryRepositoryPort {
    private val storage: MutableMap<UUID, FilmLibrary> = ConcurrentHashMap()
    private val userIdIndex: MutableMap<UUID, UUID> = ConcurrentHashMap()

    override fun save(filmLibrary: FilmLibrary): FilmLibrary {
        storage[filmLibrary.id] = filmLibrary
        userIdIndex[filmLibrary.userId] = filmLibrary.id
        return filmLibrary
    }

    override fun findById(id: UUID): FilmLibrary? {
        return storage[id]
    }

    override fun findAll(): List<FilmLibrary> {
        return storage.values.toList()
    }

    override fun deleteById(id: UUID) {
        val filmLibrary = storage[id]
        if (filmLibrary != null) {
            userIdIndex.remove(filmLibrary.userId)
        }
        storage.remove(id)
    }
}

package com.project.movienight.application.ports.input

import com.project.movienight.domain.model.FilmLibrary
import java.util.UUID

// 1. Создание бибилиотеки пользователя

interface CreateFilmLibraryUseCase {
    fun create(command: CreateFilmLibraryCommand): FilmLibrary
}

data class CreateFilmLibraryCommand(
    val userId: UUID,
    val name: String = "Мои фильмы"
)


// 2. Добавление фильма в библиотеку

interface AddFilmToLibraryUseCase {
    fun addFilm(command: AddFilmToLibraryCommand): FilmLibrary
}

data class AddFilmToLibraryCommand(
    val userId: UUID,
    val filmId: UUID
)

// 3. Удаление фильма из библиотеки

interface RemoveFilmFromLibraryUseCase {
    fun removeFilm(command: RemoveFilmFromLibraryCommand): FilmLibrary
}

data class RemoveFilmFromLibraryCommand(
    val userId: UUID,
    val filmId: UUID,
    val libraryId: UUID? = null
)

// 4. Получение библиотеки пользователя

interface GetFilmLibraryUseCase {
    fun getLibrary(query: GetFilmLibraryQuery): FilmLibrary
}

data class GetFilmLibraryQuery(
    val userId: UUID,
)

package com.project.movienight.adapters.persistence.jdbc

import com.project.movienight.adapters.persistence.jdbc.support.DelimitedValueCodec
import com.project.movienight.application.ports.output.FilmRepositoryPort
import com.project.movienight.domain.model.ContentType
import com.project.movienight.domain.model.Film
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class FilmRepository(
    private val jdbc: JdbcTemplate,
) : FilmRepositoryPort {
    private val filmRowMapper = { rs: ResultSet, _: Int ->
        Film(
            id = UUID.fromString(rs.getString("id")),
            title = rs.getString("title"),
            description = rs.getString("description"),
            contentType =
                runCatching {
                    ContentType.valueOf(
                        rs.getString("content_type"),
                    )
                }.getOrDefault(ContentType.FILM),
            releaseYear = rs.getObject("release_year")?.let { (it as Number).toInt() },
            genres = DelimitedValueCodec.decodeList(rs.getString("genres")),
            cast = DelimitedValueCodec.decodeList(rs.getString("cast_members")),
            directors = DelimitedValueCodec.decodeList(rs.getString("directors")),
            imdbRating = rs.getObject("imdb_rating")?.let { (it as Number).toDouble() },
            platformRating = rs.getObject("platform_rating")?.let { (it as Number).toDouble() },
            externalUrl = rs.getString("external_url"),
            jellyfinItemId = rs.getString("jellyfin_item_id"),
            jellyfinLibraryId = rs.getString("jellyfin_library_id"),
        )
    }

    override fun save(film: Film): Film {
        val updatedRows =
            jdbc.update(
                """
                UPDATE films
                SET title = ?, description = ?, content_type = ?, release_year = ?, genres = ?, cast_members = ?, directors = ?, imdb_rating = ?, platform_rating = ?, external_url = ?, jellyfin_item_id = ?, jellyfin_library_id = ?
                WHERE id = ?
                """.trimIndent(),
                film.title,
                film.description,
                film.contentType.name,
                film.releaseYear,
                DelimitedValueCodec.encodeList(film.genres),
                DelimitedValueCodec.encodeList(film.cast),
                DelimitedValueCodec.encodeList(film.directors),
                film.imdbRating,
                film.platformRating,
                film.externalUrl,
                film.jellyfinItemId,
                film.jellyfinLibraryId,
                film.id,
            )
        if (updatedRows == 0) {
            jdbc.update(
                """
                INSERT INTO films (id, title, description, content_type, release_year, genres, cast_members, directors, imdb_rating, platform_rating, external_url, jellyfin_item_id, jellyfin_library_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                film.id,
                film.title,
                film.description,
                film.contentType.name,
                film.releaseYear,
                DelimitedValueCodec.encodeList(film.genres),
                DelimitedValueCodec.encodeList(film.cast),
                DelimitedValueCodec.encodeList(film.directors),
                film.imdbRating,
                film.platformRating,
                film.externalUrl,
                film.jellyfinItemId,
                film.jellyfinLibraryId,
            )
        }
        return film
    }

    override fun findById(id: UUID): Film? {
        val films =
            jdbc.query(
                "SELECT id, title, description, content_type, release_year, genres, cast_members, directors, imdb_rating, platform_rating, external_url, jellyfin_item_id, jellyfin_library_id FROM films WHERE id = ?",
                filmRowMapper,
                id,
            )
        return films.firstOrNull()
    }

    override fun findByJellyfinItemId(jellyfinItemId: String): Film? {
        val films =
            jdbc.query(
                "SELECT id, title, description, content_type, release_year, genres, cast_members, directors, imdb_rating, platform_rating, external_url, jellyfin_item_id, jellyfin_library_id FROM films WHERE jellyfin_item_id = ?",
                filmRowMapper,
                jellyfinItemId,
            )
        return films.firstOrNull()
    }

    override fun findByJellyfinLibraryId(jellyfinLibraryId: String): Film? {
        val films =
            jdbc.query(
                "SELECT id, title, description, content_type, release_year, genres, cast_members, directors, imdb_rating, platform_rating, external_url, jellyfin_item_id, jellyfin_library_id FROM films WHERE jellyfin_library_id = ?",
                filmRowMapper,
                jellyfinLibraryId,
            )
        return films.firstOrNull()
    }

    override fun findAll(): List<Film> =
        jdbc.query(
            "SELECT id, title, description, content_type, release_year, genres, cast_members, directors, imdb_rating, platform_rating, external_url, jellyfin_item_id, jellyfin_library_id FROM films",
            filmRowMapper,
        )

    override fun deleteById(id: UUID) {
        jdbc.update("DELETE FROM films WHERE id = ?", id)
    }
}

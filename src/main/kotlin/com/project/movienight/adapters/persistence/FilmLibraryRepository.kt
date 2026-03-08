package com.project.movienight.adapters.persistence

import com.project.movienight.domain.model.FilmLibrary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class FilmLibraryRepository(private val jdbc: JdbcTemplate) {

    fun findById(id: Int): FilmLibrary? =
        jdbc.queryForObject(
            "SELECT * FROM favorites WHERE id = ?",
            { rs, _ ->
                FilmLibrary(
                    id = rs.getInt("id"),
                    userId = rs.getInt("userid"),
                    filmId = rs.getInt("film_id"),
                    comment = rs.getString("comment"),
                    isViewed = rs.getBoolean("is_viewed"),
                )
            },
            id
        )

    fun findAllByUserId(userId: Int): List<FilmLibrary> =
        jdbc.query(
            "SELECT * FROM favorites WHERE userid = ?",
            { rs, _ ->
                FilmLibrary(
                    id = rs.getInt("id"),
                    userId = rs.getInt("userid"),
                    filmId = rs.getInt("film_id"),
                    comment = rs.getString("comment"),
                    isViewed = rs.getBoolean("is_viewed"),
                )
            },
            userId
        )

    fun findAll(): List<FilmLibrary> =
        jdbc.query(
            "SELECT * FROM favorites"
        ) { rs, _ ->
            FilmLibrary(
                id = rs.getInt("id"),
                userId = rs.getInt("userid"),
                filmId = rs.getInt("film_id"),
                comment = rs.getString("comment"),
                isViewed = rs.getBoolean("is_viewed"),
            )
        }

    fun findTopN(limit: Int, sortBy: String = "id"): List<FilmLibrary> =
        jdbc.query(
            "SELECT * FROM favorites ORDER BY $sortBy LIMIT ?",
            { rs, _ ->
                FilmLibrary(
                    id = rs.getInt("id"),
                    userId = rs.getInt("userid"),
                    filmId = rs.getInt("film_id"),
                    comment = rs.getString("comment"),
                    isViewed = rs.getBoolean("is_viewed"),
                )
            },
            limit
        )

    fun save(library: FilmLibrary) {
        jdbc.update(
            """
            INSERT INTO favorites (userid, film_id, comment, is_viewed)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE
            SET comment = ?, is_viewed = ?
            """,
            library.userId, library.filmId, library.comment, library.isViewed,
            library.comment, library.isViewed
        )
    }

    fun deleteById(id: Int) {
        jdbc.update(
            "DELETE FROM favorites WHERE id = ?",
            id
        )
    }
}

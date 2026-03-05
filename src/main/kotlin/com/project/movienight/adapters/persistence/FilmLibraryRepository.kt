package com.project.movienight.adapters.persistence

import org.springframework.jdbc.core.JdbcTemplate

class FilmLibraryRepository(private val jdbc: JdbcTemplate) {}

package com.project.movienight.adapters.web

import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.model.ContentType

fun parseContentType(value: String): ContentType =
    runCatching { ContentType.valueOf(value.uppercase()) }
        .getOrElse { throw DomainException("Unsupported content type: $value") }

fun parseOptionalContentType(value: String?): ContentType? = value?.let { parseContentType(it) }

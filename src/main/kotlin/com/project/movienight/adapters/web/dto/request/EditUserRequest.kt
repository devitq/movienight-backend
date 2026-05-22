package com.project.movienight.adapters.web.dto.request

data class EditUserRequest(
    val name: String,
    val jellyfinUserId: String? = null,
)

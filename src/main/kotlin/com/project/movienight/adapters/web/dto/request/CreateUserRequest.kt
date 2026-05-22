package com.project.movienight.adapters.web.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,
    @field:Email
    @field:NotBlank
    @field:Size(max = 320)
    val email: String,
)

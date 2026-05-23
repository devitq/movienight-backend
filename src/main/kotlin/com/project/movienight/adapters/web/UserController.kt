package com.project.movienight.adapters.web

import com.project.movienight.adapters.security.UserPrincipal
import com.project.movienight.adapters.web.dto.request.CreateUserRequest
import com.project.movienight.adapters.web.dto.request.EditUserRequest
import com.project.movienight.adapters.web.dto.response.UserResponse
import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.ports.input.UserUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userUseCase: UserUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
    ): UserResponse =
        UserResponse.fromDomain(
            userUseCase.create(
                CreateUserCommand(
                    name = request.name,
                    email = request.email,
                ),
            ),
        )

    @GetMapping
    fun getAll(): List<UserResponse> = userUseCase.getAll().map { UserResponse.fromDomain(it) }

    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): UserResponse = UserResponse.fromDomain(userUseCase.getById(principal.getId()))

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
    ): UserResponse = UserResponse.fromDomain(userUseCase.getById(id))

    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: UUID,
        @Valid @RequestBody request: EditUserRequest,
    ): UserResponse =
        UserResponse.fromDomain(
            userUseCase.edit(
                id = id,
                command =
                    EditUserCommand(
                        name = request.name,
                        jellyfinUserId = request.jellyfinUserId,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = userUseCase.delete(id)
}

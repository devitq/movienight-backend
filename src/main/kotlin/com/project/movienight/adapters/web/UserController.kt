package com.project.movienight.adapters.web

import com.project.movienight.adapters.web.dto.request.CreateUserRequest
import com.project.movienight.adapters.web.dto.request.EditUserRequest
import com.project.movienight.adapters.web.dto.response.UserResponse
import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.CreateUserUseCase
import com.project.movienight.application.ports.input.DeleteUserUseCase
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.ports.input.EditUserUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val createUserUseCase: CreateUserUseCase,
    private val editUserUseCase: EditUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateUserRequest,
    ): UserResponse =
        UserResponse.fromDomain(
            createUserUseCase.create(
                CreateUserCommand(
                    name = request.name,
                    email = request.email,
                ),
            ),
        )

    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestBody request: EditUserRequest,
    ): UserResponse =
        UserResponse.fromDomain(
            editUserUseCase.edit(
                id = id,
                command =
                    EditUserCommand(
                        name = request.name,
                    ),
            ),
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = deleteUserUseCase.delete(id)
}

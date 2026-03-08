package com.project.movienight.adapters.web

import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.CreateUserUseCase
import com.project.movienight.application.ports.input.DeleteUserUseCase
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.ports.input.EditUserUseCase
import com.project.movienight.domain.model.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
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
    fun create(@RequestBody request: CreateUserRequest): User =
        createUserUseCase.create(
            CreateUserCommand(
                name = request.name,
                email = request.email,
            )
        )

    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestBody request: EditUserRequest,
    ): User =
        editUserUseCase.edit(
            id = id,
            command = EditUserCommand(
                name = request.name,
            )
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) =
        deleteUserUseCase.delete(id)
}

data class CreateUserRequest(
    val name: String,
    val email: String,
)

data class EditUserRequest(
    val name: String,
)

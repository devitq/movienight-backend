package com.project.movienight.adapters.web

import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(EntityNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(exception: EntityNotFoundException): ErrorResponse =
        ErrorResponse(message = exception.message ?: "Entity not found")

    @ExceptionHandler(BlockedValueException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBlockedValue(exception: BlockedValueException): ErrorResponse =
        ErrorResponse(message = exception.message ?: "Blocked value")

    @ExceptionHandler(DomainException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDomainException(exception: DomainException): ErrorResponse =
        ErrorResponse(message = exception.message ?: "Domain error")
}

data class ErrorResponse(
    val message: String,
)

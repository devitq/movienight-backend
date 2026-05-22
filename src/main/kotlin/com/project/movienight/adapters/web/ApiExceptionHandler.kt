package com.project.movienight.adapters.web

import com.project.movienight.domain.exception.BlockedValueException
import com.project.movienight.domain.exception.DomainException
import com.project.movienight.domain.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(EntityNotFoundException::class, NoResourceFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(exception: Exception): ErrorResponse {
        val traceId = currentTraceId()
        log.warn("Resource not found: traceId='{}', message='{}'", traceId, exception.message)

        return ErrorResponse(
            message = exception.message ?: "Resource not found",
            traceId = traceId,
        )
    }

    @ExceptionHandler(BlockedValueException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBlockedValue(exception: BlockedValueException): ErrorResponse {
        val traceId = currentTraceId()
        log.warn("Blocked value: traceId='{}', message='{}'", traceId, exception.message)

        return ErrorResponse(
            message = exception.message ?: "Blocked value",
            traceId = traceId,
        )
    }

    @ExceptionHandler(DomainException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDomainException(exception: DomainException): ErrorResponse {
        val traceId = currentTraceId()
        log.warn("Domain error: traceId='{}', message='{}'", traceId, exception.message)

        return ErrorResponse(
            message = exception.message ?: "Domain error",
            traceId = traceId,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(exception: MethodArgumentNotValidException): ErrorResponse {
        val traceId = currentTraceId()
        val details =
            exception
                .bindingResult
                .fieldErrors
                .joinToString("; ") { error -> "${error.field}: ${error.defaultMessage}" }
                .ifBlank { "Invalid request" }
        log.warn("Validation error: traceId='{}', message='{}'", traceId, details)

        return ErrorResponse(
            message = details,
            traceId = traceId,
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val traceId = currentTraceId()
        log.warn(
            "HTTP error: traceId='{}', status='{}', message='{}'",
            traceId,
            exception.statusCode,
            exception.reason,
        )

        return ResponseEntity
            .status(exception.statusCode)
            .body(
                ErrorResponse(
                    message = exception.reason ?: exception.message,
                    traceId = traceId,
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpectedException(exception: Exception): ErrorResponse {
        val traceId = currentTraceId()
        log.error("Unexpected error: traceId='{}'", traceId, exception)

        return ErrorResponse(
            message = "Internal server error",
            traceId = traceId,
        )
    }

    private fun currentTraceId(): String = MDC.get("traceId") ?: "unknown"
}

data class ErrorResponse(
    val message: String,
    val traceId: String,
)

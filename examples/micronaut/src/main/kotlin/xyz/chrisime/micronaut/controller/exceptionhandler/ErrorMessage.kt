package xyz.chrisime.micronaut.controller.exceptionhandler

import java.time.LocalDateTime

data class ErrorMessage(
    val message: String,
    val reason: String,
    val errorCode: ErrorCode,
    val path: String,
    val timestamp: LocalDateTime
)

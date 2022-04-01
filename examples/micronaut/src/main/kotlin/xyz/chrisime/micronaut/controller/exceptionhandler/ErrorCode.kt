package xyz.chrisime.micronaut.controller.exceptionhandler

@Suppress("UNUSED_PARAMETER")
enum class ErrorCode(value: Int, message: String) {
    NOT_RESULTS_FOUND(101, "no results found"),
    NOT_FOUND(102, "not found"),
}

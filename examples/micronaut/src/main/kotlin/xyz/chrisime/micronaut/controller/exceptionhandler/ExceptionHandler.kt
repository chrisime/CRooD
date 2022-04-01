package xyz.chrisime.micronaut.controller.exceptionhandler

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponseFactory.INSTANCE
import io.micronaut.http.HttpStatus
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton
import org.jooq.exception.DataAccessException
import java.time.LocalDateTime
import java.time.ZoneId

@Singleton
@Requires(classes = [DataAccessException::class])
class DuplicateKeyExceptionHandler : ExceptionHandler<DataAccessException, HttpResponse<ErrorMessage>> {
    override fun handle(request: HttpRequest<*>, exception: DataAccessException): HttpResponse<ErrorMessage> {
        return INSTANCE.status<ErrorMessage>(HttpStatus.CONFLICT)
            .body(
                ErrorMessage(
                    errorCode = ErrorCode.NOT_FOUND,
                    message = exception.localizedMessage,
                    reason = HttpStatus.NOT_FOUND.reason,
                    path = request.path,
                    timestamp = LocalDateTime.now(ZoneId.of("UTC"))
                )
            )
    }
}

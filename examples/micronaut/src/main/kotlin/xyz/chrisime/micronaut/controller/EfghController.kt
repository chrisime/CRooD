package xyz.chrisime.micronaut.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.ok
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import xyz.chrisime.micronaut.domain.EfghDomain
import xyz.chrisime.micronaut.dto.EfghUpdateDto
import xyz.chrisime.micronaut.service.business.EfghService
import java.util.UUID
import java.util.stream.Stream

@Validated
@Controller("/api/v1/efgh")
class EfghController(private val efghService: EfghService) {

    @Get
    fun getAll(): HttpResponse<Stream<EfghDomain>> {
        return ok(efghService.getAll())
    }

    @Get("/{identifier}")
    fun getByIdentifier(@PathVariable identifier: UUID): HttpResponse<EfghDomain> {
        return ok(efghService.getByIdentifier(identifier))
    }

    @Put
    fun updateItem(@Valid @Body update: EfghUpdateDto): HttpResponse<Boolean> {
        val result = efghService.update(identifier = update.identifier.toString(), update.email)
        return ok(result > 0)
    }

    @Delete("/{identifier}")
    fun deleteItem(@PathVariable("identifier") identifier: UUID): HttpResponse<Boolean> {
        val result = efghService.delete(identifier)

        return ok(result > 0)
    }

    @Get("/id/{id}")
    fun exists(@PathVariable id: UUID): HttpResponse<Boolean>? {
        return ok(efghService.existsByPk(id))
    }

}

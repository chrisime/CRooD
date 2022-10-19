package xyz.chrisime.micronaut.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.ok
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import xyz.chrisime.micronaut.dto.AbcdDto
import xyz.chrisime.micronaut.dto.AbcdEfghDto
import xyz.chrisime.micronaut.dto.UpdateAbcdDto
import xyz.chrisime.micronaut.service.business.AbcdService

@Validated
@Controller("/api/v1/abcd")
class AbcdController(private val abcdService: AbcdService) {

    @Get
    fun receiveAll(): List<AbcdEfghDto> {
        return abcdService.getAll()
    }

    @Post
    @Status(HttpStatus.CREATED)
    fun createOne(@Valid @Body create: AbcdDto) {
        abcdService.create(create.name, create.amount.toShort(), create.emails)
    }

    @Put("/{identifier}")
    @Status(HttpStatus.ACCEPTED)
    fun updateOne(@Valid @Body update: UpdateAbcdDto, @PathVariable identifier: String) {
        abcdService.update(identifier, update.isAvailable, update.amount.toShort())
    }

    @Put
    @Status(HttpStatus.ACCEPTED)
    fun setUnavailable() {
        abcdService.setAllToUnavailable()
    }

    @Get("/id/{id}")
    fun exists(@PathVariable id: Long): HttpResponse<Boolean>? {
        return ok(abcdService.exists(id))
    }

}

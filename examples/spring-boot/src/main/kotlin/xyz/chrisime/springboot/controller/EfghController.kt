package xyz.chrisime.springboot.controller

import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.chrisime.springboot.domain.EfghDomain
import xyz.chrisime.springboot.dto.EfghUpdateDto
import xyz.chrisime.springboot.service.business.EfghService
import java.util.*
import java.util.stream.Stream

@RestController
@RequestMapping("/api/v1/efgh")
class EfghController(private val efghService: EfghService) {

    @GetMapping
    fun getAll(): ResponseEntity<Stream<EfghDomain>> {
        return ok(efghService.getAll())
    }

    @GetMapping("/{identifier}")
    fun getByIdentifier(@PathVariable identifier: UUID): ResponseEntity<EfghDomain> {
        return ok(efghService.getByIdentifier(identifier))
    }

    @PutMapping
    fun updateItem(@Validated @RequestBody update: EfghUpdateDto): ResponseEntity<Boolean> {
        val result = efghService.update(identifier = update.identifier.toString(), update.email)
        return ok(result > 0)
    }

    @DeleteMapping("/{identifier}")
    fun deleteItem(@PathVariable("identifier") identifier: UUID): ResponseEntity<Boolean> {
        val result = efghService.delete(identifier)

        return ok(result > 0)
    }

//    @GetMapping("/id/{id}")
//    fun exists(@PathVariable id: UUID): ResponseEntity<Boolean>? {
//        return ok(efghService.existsByPk(id))
//    }

}

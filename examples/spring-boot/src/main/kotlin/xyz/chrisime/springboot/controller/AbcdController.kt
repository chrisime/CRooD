package xyz.chrisime.springboot.controller

import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.chrisime.springboot.dto.AbcdDto
import xyz.chrisime.springboot.dto.AbcdEfghDto
import xyz.chrisime.springboot.dto.UpdateAbcdDto
import xyz.chrisime.springboot.service.business.AbcdService

@RestController
@RequestMapping("/api/v1/abcd")
class AbcdController(private val abcdService: AbcdService) {

    @GetMapping
    fun receiveAll(): List<AbcdEfghDto> {
        return abcdService.getAll()
    }

    @PostMapping
    fun createOne(@Validated @RequestBody create: AbcdDto) {
        abcdService.create(create.name, create.amount.toShort(), create.emails)
    }

    @PutMapping("/{identifier}")
    fun updateOne(@Validated @RequestBody update: UpdateAbcdDto, @PathVariable identifier: String) {
        abcdService.update(identifier, update.isAvailable, update.amount.toShort())
    }

    @PutMapping
    fun setUnavailable() {
        abcdService.setAllToUnavailable()
    }

    @GetMapping("/id/{id}")
    fun exists(@PathVariable id: Long): ResponseEntity<Boolean>? {
        return ok(abcdService.exists(id))
    }

}

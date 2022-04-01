package xyz.chrisime.springboot.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.*

data class EfghUpdateDto(
    @field:NotBlank @field:NotNull val identifier: UUID,
    @field:Size(min = 3, max = 31) @field:NotBlank val email: String,
)

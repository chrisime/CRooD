package xyz.chrisime.micronaut.dto

import io.micronaut.core.annotation.Introspected
import java.util.UUID
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Introspected
data class EfghUpdateDto(
    @field:NotBlank @field:NotNull val identifier: UUID,
    @field:Size(min = 3, max = 31) @field:NotBlank val email: String,
)

package xyz.chrisime.micronaut.dto

import io.micronaut.core.annotation.Introspected

@Introspected
data class AbcdDto(val name: String, val amount: Int, val emails: Set<String>)

@Introspected
data class UpdateAbcdDto(val amount: Int, val isAvailable: Boolean)

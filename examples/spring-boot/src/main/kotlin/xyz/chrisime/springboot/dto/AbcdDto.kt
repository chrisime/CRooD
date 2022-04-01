package xyz.chrisime.springboot.dto

data class AbcdDto(val name: String, val amount: Int, val emails: Set<String>)

data class UpdateAbcdDto(val amount: Int, val isAvailable: Boolean)

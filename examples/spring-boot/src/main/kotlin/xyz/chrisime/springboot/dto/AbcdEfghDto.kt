package xyz.chrisime.springboot.dto

import java.util.*
import java.beans.Transient

data class AbcdEfghDto(
    val abcdIdentifier: UUID,
    val efghIdentifier: UUID,
    val available: Boolean,
    val nickName: String,
    val amount: Short,
    val emails: String,
    @get:Transient val version: Long
)

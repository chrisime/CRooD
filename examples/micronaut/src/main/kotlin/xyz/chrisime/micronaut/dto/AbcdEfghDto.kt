package xyz.chrisime.micronaut.dto

import java.util.*
import java.beans.Transient
import javax.persistence.Version

data class AbcdEfghDto(
    val abcdIdentifier: UUID,
    val efghIdentifier: UUID,
    val available: Boolean,
    val nickName: String,
    val amount: Short,
    val emails: String,
    @Version @get:Transient val version: Long
)

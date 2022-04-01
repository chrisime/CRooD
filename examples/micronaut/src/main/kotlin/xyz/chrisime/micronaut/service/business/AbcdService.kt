package xyz.chrisime.micronaut.service.business

import io.micronaut.transaction.annotation.ReadOnly
import jakarta.inject.Singleton
import xyz.chrisime.micronaut.domain.AbcdDomain
import xyz.chrisime.micronaut.domain.EfghDomain
import xyz.chrisime.micronaut.dto.AbcdEfghDto
import xyz.chrisime.micronaut.service.persistence.AbcdRepository
import xyz.chrisime.micronaut.service.persistence.EfghRepository
import java.util.*
import javax.transaction.Transactional

@Singleton
open class AbcdService(
    private val abcdRepository: AbcdRepository,
    private val efghRepository: EfghRepository
) {

    @ReadOnly
    open fun getAll(): List<AbcdEfghDto> {
        return abcdRepository.findThem()
    }

    fun update(identifier: String, isAvailable: Boolean, amount: Short) {
        abcdRepository.updateOne(UUID.fromString(identifier), isAvailable, amount)
    }

    fun setAllToUnavailable() {
        abcdRepository.setAllToUnavailable()
    }

    @Transactional
    open fun create(name: String, _amount: Short, emails: Set<String>) {
        val abcdId = abcdRepository.create(
            AbcdDomain(identifier = UUID.randomUUID(), nickname = name, amount = _amount, isAvailable = false)
        )

        emails.forEach { email ->
            efghRepository.create(
                EfghDomain(identifier = UUID.randomUUID(), email = email, abcdId = abcdId)
            )
        }
    }

    @ReadOnly
    open fun exists(id: Long): Boolean {
        return abcdRepository.existsById(id)
    }

}

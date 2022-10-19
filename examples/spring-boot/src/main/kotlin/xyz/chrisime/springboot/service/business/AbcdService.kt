package xyz.chrisime.springboot.service.business

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.chrisime.springboot.dto.AbcdEfghDto
import xyz.chrisime.springboot.service.persistence.AbcdRepository
import xyz.chrisime.springboot.service.persistence.EfghRepository
import java.util.*

@Service
class AbcdService(
    @Autowired private val abcdRepository: AbcdRepository,
    @Autowired private val efghRepository: EfghRepository
) {

    fun getAll(): List<AbcdEfghDto> {
        return abcdRepository.findThem()
    }

    fun update(identifier: String, isAvailable: Boolean, amount: Short) {
        abcdRepository.updateOne(UUID.fromString(identifier), isAvailable, amount)
    }

    fun setAllToUnavailable() {
        abcdRepository.setAllToUnavailable()
    }

    @Transactional
    fun create(name: String, _amount: Short, emails: Set<String>) {
        val abcdId = abcdRepository.create(name, _amount)

        efghRepository.createEmails(emails, abcdId)
    }

    fun exists(id: Long): Boolean {
        return abcdRepository.exists(id)
    }

}

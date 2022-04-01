package xyz.chrisime.springboot.service.business

import org.springframework.stereotype.Service
import xyz.chrisime.springboot.domain.EfghDomain
import xyz.chrisime.springboot.service.persistence.EfghRepository
import java.util.*
import java.util.stream.Stream

@Service
class EfghService(private val efghRepository: EfghRepository) {

    fun getAll(): Stream<EfghDomain> {
        return efghRepository.find()
    }

    fun getByIdentifier(identifier: UUID): EfghDomain {
        return efghRepository.findByIdentifier(identifier)
    }

    fun update(identifier: String, email: String): Int {
        return efghRepository.updateByIdentifier(UUID.fromString(identifier), email)
    }

    fun delete(identifier: UUID): Int {
        return efghRepository.deleteByIdentifier(identifier)
    }

    fun existsByPk(pk: UUID): Boolean {
        return efghRepository.existsById(pk)
    }

}

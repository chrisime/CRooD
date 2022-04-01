package xyz.chrisime.springboot.service.persistence

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.chrisime.crood.service.CRooDService
import xyz.chrisime.springboot.domain.AbcdDomain
import xyz.chrisime.springboot.dto.AbcdEfghDto
import xyz.chrisime.springboot.tables.records.AbcdRecord
import xyz.chrisime.springboot.tables.references.ABCD
import xyz.chrisime.springboot.tables.references.EFGH
import java.util.*

@Repository
class AbcdRepository(@Autowired private val dsl: DSLContext) : CRooDService<AbcdRecord, Long, AbcdDomain>(dsl) {

    @Transactional
    fun updateOne(uuid: UUID, isAvailable: Boolean, amount: Short) {
        update(ABCD.IS_AVAILABLE, isAvailable, ABCD.AMOUNT, amount) {
            where(ABCD.IDENTIFIER.eq(uuid))
        }
    }

    @Transactional
    fun setAllToUnavailable() {
        update(ABCD.IS_AVAILABLE, false) {
            where(ABCD.IS_AVAILABLE.isTrue)
        }
    }

    fun findThem(): List<AbcdEfghDto> {
        return dsl.select(
            ABCD.IDENTIFIER, ABCD.NICKNAME, ABCD.AMOUNT, ABCD.IS_AVAILABLE,
            EFGH.IDENTIFIER, EFGH.EMAIL, ABCD.VERSION
        )
            .from(ABCD)
            .join(EFGH).on(ABCD.ID.eq(EFGH.ABCD_ID))
            .map {
                AbcdEfghDto(
                    it.get(ABCD.IDENTIFIER)!!,
                    it.get(EFGH.IDENTIFIER)!!,
                    it.get(ABCD.IS_AVAILABLE)!!,
                    it.get(ABCD.NICKNAME)!!,
                    it.get(ABCD.AMOUNT)!!,
                    it.get(EFGH.EMAIL)!!,
                    it.get(ABCD.VERSION)!!
                )
            }
    }

    @Transactional
    fun create(name: String, quantity: Short): Long {
        return create(AbcdDomain(identifier = UUID.randomUUID(), nickname = name, amount = quantity, isAvailable = false))
    }

    fun exists(id: Long): Boolean {
        return existsById(id)
    }

}

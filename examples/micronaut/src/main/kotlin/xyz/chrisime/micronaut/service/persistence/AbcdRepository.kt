package xyz.chrisime.micronaut.service.persistence

import jakarta.inject.Singleton
import org.jooq.DSLContext
import xyz.chrisime.crood.service.CRooDService
import xyz.chrisime.micronaut.domain.AbcdDomain
import xyz.chrisime.micronaut.dto.AbcdEfghDto
import xyz.chrisime.micronaut.tables.records.AbcdRecord
import xyz.chrisime.micronaut.tables.references.ABCD
import xyz.chrisime.micronaut.tables.references.EFGH
import java.util.*
import javax.transaction.Transactional

@Singleton
open class AbcdRepository(private val dsl: DSLContext) : CRooDService<AbcdRecord, Long, AbcdDomain>(dsl) {

    init {
        dsl.settings().withExecuteWithOptimisticLocking(true)
    }

    @Transactional
    open fun updateOne(uuid: UUID, isAvailable: Boolean, amount: Short) {
        update(ABCD.IS_AVAILABLE, isAvailable, ABCD.AMOUNT, amount) {
            where(ABCD.IDENTIFIER.eq(uuid))
        }
    }

    @Transactional
    open fun setAllToUnavailable() {
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

}

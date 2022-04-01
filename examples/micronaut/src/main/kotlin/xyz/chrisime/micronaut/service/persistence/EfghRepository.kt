package xyz.chrisime.micronaut.service.persistence

import jakarta.inject.Singleton
import org.jooq.DSLContext
import org.jooq.impl.DSL
import xyz.chrisime.crood.service.CRooDService
import xyz.chrisime.micronaut.domain.EfghDomain
import xyz.chrisime.micronaut.tables.records.EfghRecord
import xyz.chrisime.micronaut.tables.references.EFGH
import java.util.*
import java.util.stream.Stream
import javax.transaction.Transactional

@Singleton
open class EfghRepository(dsl: DSLContext) : CRooDService<EfghRecord, UUID, EfghDomain>(dsl) {

    fun find(): Stream<EfghDomain> {
        return findAll {
            where(DSL.one().eq(DSL.one()))
        }
    }

    fun findByIdentifier(identifier: UUID): EfghDomain {
        return findOne {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

    fun findEMailByIdentifier(identifier: UUID): String? {
        return fetchOne(EFGH.EMAIL) {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

    @Transactional
    open fun updateByIdentifier(identifier: UUID, email: String): Int {
        return update(EFGH.EMAIL, email) {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

    @Transactional
    open fun deleteByIdentifier(identifier: UUID): Int {
        return delete {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

}

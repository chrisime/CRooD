package xyz.chrisime.springboot.service.persistence

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import xyz.chrisime.crood.service.CRooDService
import xyz.chrisime.springboot.domain.EfghDomain
import xyz.chrisime.springboot.tables.records.EfghRecord
import xyz.chrisime.springboot.tables.references.EFGH
import java.util.*
import java.util.stream.Stream

@Repository
class EfghRepository(@Autowired dsl: DSLContext) : CRooDService<EfghRecord, UUID, EfghDomain>(dsl) {

    @Transactional
    fun createEmails(emails: Set<String>, abcdId: Long) {
        emails.forEach { email ->
            create(EfghDomain(identifier = UUID.randomUUID(), email = email, abcdId = abcdId))
        }
    }

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
    fun updateByIdentifier(identifier: UUID, email: String): Int {
        return update(EFGH.EMAIL, email) {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

    fun exists(identifier: UUID) {
        exists {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

    @Transactional
    fun deleteByIdentifier(identifier: UUID): Int {
        return delete {
            where(EFGH.IDENTIFIER.eq(identifier))
        }
    }

}

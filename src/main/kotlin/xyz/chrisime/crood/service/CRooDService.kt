/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.chrisime.crood.service

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SelectConditionStep
import org.jooq.SelectWhereStep
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableRecord
import org.jooq.UniqueKey
import org.jooq.UpdatableRecord
import org.jooq.exception.DataAccessException
import xyz.chrisime.crood.domain.IdentifiableDomain
import xyz.chrisime.crood.extensions.asType
import xyz.chrisime.crood.extensions.getClassAtIndex
import xyz.chrisime.crood.extensions.newInstance
import xyz.chrisime.crood.id.PrimaryKey
import java.util.*
import java.util.stream.Stream

/**
 * Common service based on jOOQ that provides basic CRUD operations.
 *
 * @param R record type: a record contains a table row's data
 * @param ID table identifier type (primary key)
 * @param D domain type: a domain contains the values of a single table row
 *
 * @author Christian Meyer &ltchristian.meyer@gmail.com:gt;
 */
abstract class CRooDService<R, ID, D>(private val dsl: DSLContext)
    where R : TableRecord<R>, ID : Any, D : IdentifiableDomain {

    private val tableRecord = newInstance<R>()

    private val table = tableRecord.table

    private val pkFields = table.keys
        .filter(UniqueKey<R>::isPrimary)
        .flatMap(UniqueKey<R>::getFields)
        .toTypedArray()
        .asType<Array<TableField<R, ID>>>()

    private val domain = getClassAtIndex<D>(2)

    init {
        with(table.recordType()) {
            val configuration = dsl.configuration()
            configuration.recordUnmapperProvider().provide(domain, this)
            configuration.recordMapperProvider().provide(this, domain)
        }
    }

    @Throws(DataAccessException::class)
    fun selectCount(): Int = dsl.selectCount().from(table).fetchSingleInto(Int::class.javaPrimitiveType)

    @Throws(DataAccessException::class)
    fun selectCount(tableOps: Table<R>.() -> Table<R>): Int =
        dsl.selectCount().from(this.table.tableOps()).fetchSingleInto(Int::class.javaPrimitiveType)

    @Throws(DataAccessException::class)
    fun existsById(id: ID): Boolean = dsl.fetchExists(table, PrimaryKey(id).equal(*pkFields))

    @Throws(DataAccessException::class)
    fun exists(tableOps: Table<R>.() -> Table<R>): Boolean = dsl.fetchExists(this.table.tableOps())

    @Throws(DataAccessException::class)
    fun <F> fetchOne(field: Field<F>, whereStep: SelectWhereStep<R>.() -> SelectConditionStep<R>): F =
        dsl.selectFrom(table).whereStep().fetchSingleInto(field.type)

    @Throws(DataAccessException::class)
    fun <F> fetchOptional(field: Field<F>, whereStep: SelectWhereStep<R>.() -> SelectConditionStep<R>): Optional<F> =
        dsl.selectFrom(table).whereStep().fetchOptionalInto(field.type)

    @Throws(DataAccessException::class)
    fun <F> fetchAll(field: Field<F>, whereStep: SelectWhereStep<R>.() -> SelectConditionStep<R>): Stream<F> =
        dsl.selectFrom(table).whereStep().fetchStreamInto(field.type)

    @Throws(DataAccessException::class)
    fun findById(id: ID): D = dsl.selectFrom(table).where(PrimaryKey(id).equal(*pkFields)).fetchSingleInto(domain)

    @Throws(DataAccessException::class)
    fun findAll(): Stream<D> = dsl.selectFrom(table).fetchStreamInto(domain)

    @Throws(DataAccessException::class)
    fun findAll(whereStep: SelectWhereStep<R>.() -> SelectConditionStep<R>): Stream<D> =
        dsl.selectFrom(table).whereStep().fetchStreamInto(domain)

    @Throws(DataAccessException::class)
    fun findOne(whereStep: SelectWhereStep<R>.() -> SelectConditionStep<R>): D =
        dsl.selectFrom(table).whereStep().fetchSingleInto(domain)

    @Throws(DataAccessException::class)
    fun findOptional(whereStep: SelectWhereStep<R>.() -> SelectConditionStep<R>): Optional<D> =
        dsl.selectFrom(table).whereStep().fetchOptionalInto(domain)

    @Throws(DataAccessException::class)
    fun findOptionalById(id: ID): Optional<D> = dsl.selectFrom(table).where(PrimaryKey(id).equal(*pkFields)).fetchOptionalInto(domain)

    @Throws(DataAccessException::class)
    fun create(vararg sources: D): Long = create(sources.toList())

    @Throws(DataAccessException::class)
    fun create(source: D): Long {
        dsl.newRecord(table, source).insert()
        return dsl.lastID().longValueExact()
    }

    @Throws(DataAccessException::class)
    fun create(sources: Collection<D>): Long = when {
        sources.isEmpty() -> 0
        sources.size == 1 -> create(sources.iterator().next())
        else -> sources.map(::create).reduce { acc, l -> acc + l }
    }

    @Throws(DataAccessException::class)
    fun <F> update(
        field1: Field<F>,
        value1: F,
        tableOps: Table<R>.() -> Table<R>
    ): Int {
        val records = dsl.fetch(this.table.tableOps()).map {
            it.with(field1, value1)
        }

        return records.storeOrBatchUpdate(field1)
    }

    @Throws(DataAccessException::class)
    fun <F, G> update(
        field1: Field<F>,
        value1: F,
        field2: Field<G>,
        value2: G,
        tableOps: Table<R>.() -> Table<R>
    ): Int {
        val records = dsl.fetch(this.table.tableOps()).map {
            it.with(field1, value1).with(field2, value2)
        }

        return records.storeOrBatchUpdate(field1, field2)
    }

    @Throws(DataAccessException::class)
    fun <F, G, H> update(
        field1: Field<F>,
        value1: F,
        field2: Field<G>,
        value2: G,
        field3: Field<H>,
        value3: H,
        tableOps: Table<R>.() -> Table<R>
    ): Int {
        val records = dsl.fetch(this.table.tableOps()).map {
            it.with(field1, value1).with(field2, value2).with(field3, value3)
        }

        return records.storeOrBatchUpdate(field1, field2, field3)
    }

    @Throws(DataAccessException::class)
    fun <F, G, H, I> update(
        field1: Field<F>,
        value1: F,
        field2: Field<G>,
        value2: G,
        field3: Field<H>,
        value3: H,
        field4: Field<I>,
        value4: I,
        tableOps: Table<R>.() -> Table<R>
    ): Int {
        val records = dsl.fetch(this.table.tableOps()).map {
            it.with(field1, value1).with(field2, value2).with(field3, value3).with(field4, value4)
        }

        return records.storeOrBatchUpdate(field1, field2, field3, field4)
    }

    @Throws(DataAccessException::class)
    fun <F, G, H, I, J> update(
        field1: Field<F>,
        value1: F,
        field2: Field<G>,
        value2: G,
        field3: Field<H>,
        value3: H,
        field4: Field<I>,
        value4: I,
        field5: Field<J>,
        value5: J,
        tableOps: Table<R>.() -> Table<R>
    ): Int {
        val records = dsl.fetch(this.table.tableOps()).map {
            it.with(field1, value1).with(field2, value2).with(field3, value3).with(field4, value4).with(field5, value5)
        }

        return records.storeOrBatchUpdate(field1, field2, field3, field4, field5)
    }

    @Throws(DataAccessException::class)
    fun deleteById(id: ID): Int = dsl.deleteFrom(table).where(PrimaryKey(id).equal(*pkFields)).execute()

    @Throws(DataAccessException::class)
    fun delete(tableOps: Table<R>.() -> Table<R>): Int = dsl.deleteFrom(this.table.tableOps()).execute()

    @Throws(DataAccessException::class)
    fun truncate(): Int = dsl.truncate(table).restartIdentity().cascade().execute()

    @Throws(DataAccessException::class)
    private fun List<TableRecord<R>>.storeOrBatchUpdate(vararg fields: Field<*>): Int {
        return if (this.isEmpty()) {
            0
        } else if (this.size == 1) {
            first().asType<UpdatableRecord<*>>().store(*fields)
        } else {
            dsl.batchUpdate(this.asType<List<UpdatableRecord<*>>>()).execute().fold(0) { acc, i -> acc + i }
        }
    }

}

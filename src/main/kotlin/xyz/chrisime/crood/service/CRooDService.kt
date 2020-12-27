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

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableField
import org.jooq.UpdatableRecord
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import xyz.chrisime.crood.domain.IdentifiableDomain
import xyz.chrisime.crood.error.GenericError
import xyz.chrisime.crood.extensions.asType
import xyz.chrisime.crood.extensions.getClassAtIndex
import xyz.chrisime.crood.extensions.newInstance
import xyz.chrisime.crood.id.Identifier
import java.util.Optional
import java.util.stream.Stream

/**
 * Common service based on jOOQ that provides basic CRUD operations.
 *
 * @param R table record type: a record contains a row's data
 * @param ID table identifier type (primary key)
 * @param D domain type: a domain contains the values of a single table row
 *
 * @author Christian Meyer &ltchristian.meyer@gmail.com:gt;
 */
abstract class CRooDService<R : UpdatableRecord<R>, ID : Any, D : IdentifiableDomain>(private val dsl: DSLContext) {

    private val rTable: Table<R>

    private val cDomain: Class<D>

    private val pkFields: Array<TableField<Record, ID>>

    init {
        dsl.settings().isRenderFormatted = true

        rTable = newInstance<UpdatableRecord<R>>().table
        pkFields = rTable.getPrimaryKeys()
        cDomain = getClassAtIndex(2)

        dsl.configuration().recordUnmapperProvider().provide(cDomain, rTable.recordType())
        dsl.configuration().recordMapperProvider().provide(rTable.recordType(), cDomain)
    }

    /**
     * Returns count of rows in table filtered by given condition.
     * @param condition predicate to filter result for
     */
    fun fetchCountWhere(condition: () -> Condition): Int =
        dsl.selectCount().from(rTable).where(condition()).fetchOne(0, Int::class.javaPrimitiveType)
            ?: throw GenericError.Database("no results in ${rTable.name}")

    fun <F> fetchOne(field: Field<F>, value: F): F = try {
        dsl.select(field).from(rTable).where(field.eq(value)).fetchOneInto(field.type)
            ?: throw GenericError.Database("no results in ${rTable.name}")
    } catch (dae: DataAccessException) {
        throw GenericError.Database(dae.message, dae.cause)
    }

    fun <F> fetchOptional(field: Field<F>, value: F): Optional<F> =
        dsl.select(field).from(rTable).where(field.eq(value)).fetchOptionalInto(field.type)

    fun <F> fetchAll(field: Field<F>, vararg values: F): Stream<F> =
        dsl.select(field).from(rTable).where(field.`in`(*values)).fetchStreamInto(field.type)

    fun <F> fetchOneWhere(field: Field<F>, condition: () -> Condition): F = try {
        dsl.select(field).from(rTable).where(condition()).fetchOneInto(field.type)
            ?: throw GenericError.Database("no results in ${rTable.name}")
    } catch (dae: DataAccessException) {
        throw GenericError.Database(dae.message, dae.cause)
    }

    fun <F> fetchOptionalWhere(field: Field<F>, condition: () -> Condition): Optional<F> =
        dsl.select(field).from(rTable).where(condition()).fetchOptionalInto(field.type)

    fun <F> fetchAllWhere(field: Field<F>, condition: () -> Condition): Stream<F> =
        dsl.select(field).from(rTable).where(condition()).fetchStreamInto(field.type)

    fun findAll(): Stream<D> = dsl.selectFrom(rTable).fetchStreamInto(cDomain)

    fun findAllWhere(condition: () -> Condition): Stream<D> =
        dsl.selectFrom(rTable).where(condition()).fetchStreamInto(cDomain)

    fun findOneWhere(condition: () -> Condition): D = try {
        dsl.selectFrom(rTable).where(condition()).fetchOneInto(cDomain)
            ?: throw GenericError.Database("no results in ${rTable.name}")
    } catch (dae: DataAccessException) {
        throw GenericError.Database(dae.message, dae.cause)
    }

    fun findOptionalWhere(condition: () -> Condition): Optional<D> =
        dsl.selectFrom(rTable).where(condition()).fetchOptionalInto(cDomain)

    fun findById(id: ID): D = dsl.selectFrom(rTable).where(id.isEqual()).fetchOneInto(cDomain)
        ?: throw GenericError.Database("no results in ${rTable.name}")

    fun findOptionalById(id: ID): Optional<D> =
        dsl.selectFrom(rTable).where(id.isEqual()).fetchOptionalInto(cDomain)

    fun existsById(id: ID): Boolean = dsl.fetchExists(dsl.selectFrom(rTable).where(id.isEqual()))

    fun existsWhere(condition: () -> Condition): Boolean =
        dsl.fetchExists(dsl.selectFrom(rTable).where(condition()))

    fun create(source: D): Int = dsl.newRecord(rTable, source).insert()

    fun create(vararg sources: D): IntArray = create(sources.toList())

    fun create(sources: Collection<D>): IntArray = when {
        sources.isEmpty() -> intArrayOf(0)
        sources.size == 1 -> intArrayOf(create(sources.iterator().next()))
        else -> sources.map { src -> dsl.newRecord(rTable, src) }.map { r -> r.insert() }.toIntArray()
    }

    fun update(vararg sources: D): IntArray = update(sources.toList())

    fun update(sources: Collection<D>): IntArray = updateOrDelete(sources, Operation.UPDATE)

    fun deleteById(id: ID): Int = dsl.deleteFrom(rTable).where(id.isEqual()).execute()

    fun deleteWhere(condition: () -> Condition): Int = dsl.deleteFrom(rTable).where(condition()).execute()

    fun delete(vararg sources: D): IntArray = delete(sources.toList())

    fun delete(sources: Collection<D>): IntArray = updateOrDelete(sources, Operation.DELETE)

    fun truncate(): Int = dsl.truncate(rTable).restartIdentity().cascade().execute()

    private fun setFieldForUpdateOrDelete(domains: Iterable<D>): List<UpdatableRecord<*>> = domains.map {
        with(dsl.newRecord(rTable, it)) {
            pkFields.forEach { pkField ->
                changed(pkField, false)
            }
            this
        }
    }

    private fun updateOrDelete(sources: Collection<D>, operation: Operation): IntArray = when {
        sources.isEmpty() -> intArrayOf()
        else -> setFieldForUpdateOrDelete(sources).let {
            when (it.size) {
                1 ->
                    when (operation) {
                        Operation.DELETE -> intArrayOf(it[0].delete())
                        Operation.UPDATE -> intArrayOf(it[0].update())
                    }
                else -> when (operation) {
                    Operation.DELETE -> it.map { r -> r.delete() }.toIntArray()
                    Operation.UPDATE -> it.map { r -> r.update() }.toIntArray()
                }
            }
        }
    }

    private fun ID.isEqual(): Condition = when (val id = this) {
        is Identifier -> DSL.row(*pkFields).eq(DSL.row(*id.identifier))
        else -> pkFields[0].eq(pkFields[0].dataType.convert(id))
    }

    private fun Table<*>.getPrimaryKeys() = this.keys
        .filter { it.isPrimary }
        .flatMap { it.fields.asType<List<TableField<Record, ID>>>() }.toTypedArray()

    private enum class Operation {
        UPDATE, DELETE
    }

}

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

import java.util.*
import java.util.stream.Stream
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Table
import org.jooq.TableField
import org.jooq.UpdatableRecord
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import xyz.chrisime.crood.domain.IdentifiableDomain
import xyz.chrisime.crood.error.GenericError
import xyz.chrisime.crood.extensions.GenericExt.getClassAtIndex
import xyz.chrisime.crood.extensions.GenericExt.new
import xyz.chrisime.crood.extensions.asType
import xyz.chrisime.crood.id.Identifier

/**
 * Common service based on jOOQ that provides basic CRUD operations.
 *
 * @param R table record type: a record contains a row's data
 * @param ID table identifier type (primary key)
 * @param D domain type: a domain contains the values of a single table row
 *
 * @author Christian Meyer &ltchristian.meyer@gmail.com:gt;
 */
abstract class CRUDService<R : UpdatableRecord<R>, ID : Any, D : IdentifiableDomain>(private val dsl: DSLContext) {

    private val rTable: Table<R>

    private val cDomain: Class<D>

    private val pkFields: List<TableField<R, ID>>

    init {
        // TODO: batch operations are not fully supported yet, see https://github.com/jOOQ/jOOQ/issues/5383
        dsl.settings().isExecuteWithOptimisticLocking = true
        dsl.settings().isRenderFormatted = true

        rTable = new<UpdatableRecord<R>>(0).table
        pkFields = rTable.keys.filter { it.isPrimary }.map { it.fieldsArray.asType<TableField<R, ID>>() }
        cDomain = getClassAtIndex(2)

        dsl.configuration().recordUnmapperProvider().provide(cDomain, rTable.recordType())
        dsl.configuration().recordMapperProvider().provide(rTable.recordType(), cDomain)
    }

    protected fun fetchCountWhere(condition: () -> Condition): Int =
        dsl.selectCount().from(rTable).where(condition()).fetchOne(0, Int::class.javaPrimitiveType)
                ?: throw GenericError.Database("no results in ${rTable.name}")

    protected fun <F> fetchOne(field: Field<F>, value: F): F = try {
        dsl.select(field).from(rTable).where(field.eq(value)).fetchOneInto(field.type)
                ?: throw GenericError.Database("no results in ${rTable.name}")
    } catch (dae: DataAccessException) {
        throw GenericError.Database(dae.message, dae.cause)
    }

    protected fun <F> fetchOptional(field: Field<F>, value: F): Optional<F> =
        dsl.select(field).from(rTable).where(field.eq(value)).fetchOptionalInto(field.type)

    protected fun <F> fetchAll(field: Field<F>, vararg values: F): Sequence<F> =
        dsl.select(field).from(rTable).where(field.`in`(*values)).fetchStreamInto(field.type).toSequence()

    protected fun <F> fetchOneWhere(field: Field<F>, condition: () -> Condition): F = try {
        dsl.select(field).from(rTable).where(condition()).fetchOneInto(field.type)
                ?: throw GenericError.Database("no results in ${rTable.name}")
    } catch (dae: DataAccessException) {
        throw GenericError.Database(dae.message, dae.cause)
    }

    protected fun <F> fetchOptionalWhere(field: Field<F>, condition: () -> Condition): Optional<F> =
        dsl.select(field).from(rTable).where(condition()).fetchOptionalInto(field.type)

    protected fun <F> fetchAllWhere(field: Field<F>, condition: () -> Condition): Sequence<F> =
        dsl.select(field).from(rTable).where(condition()).fetchStreamInto(field.type).toSequence()

    protected fun findAll(): Sequence<D> = dsl.selectFrom(rTable).fetchStreamInto(cDomain).toSequence()

    protected fun findAllWhere(condition: () -> Condition): Sequence<D> =
        dsl.selectFrom(rTable).where(condition()).fetchStreamInto(cDomain).toSequence()

    protected fun findOneWhere(condition: () -> Condition): D = try {
        dsl.selectFrom(rTable).where(condition()).fetchOneInto(cDomain)
                ?: throw GenericError.Database("no results in ${rTable.name}")
    } catch (dae: DataAccessException) {
        throw GenericError.Database(dae.message, dae.cause)
    }

    protected fun findOptionalWhere(condition: () -> Condition): Optional<D> =
        dsl.selectFrom(rTable).where(condition()).fetchOptionalInto(cDomain)

    protected fun findById(id: ID): D = dsl.selectFrom(rTable).where(id.equal()).fetchOneInto(cDomain)
            ?: throw GenericError.Database("no results in ${rTable.name}")

    protected fun findOptionalById(id: ID): Optional<D> =
        dsl.selectFrom(rTable).where(id.equal()).fetchOptionalInto(cDomain)

    protected fun existsById(id: ID): Boolean = dsl.fetchExists(dsl.selectFrom(rTable).where(id.equal()))

    protected fun existsWhere(condition: () -> Condition): Boolean =
        dsl.fetchExists(dsl.selectFrom(rTable).where(condition()))

    protected fun create(source: D): Int = dsl.newRecord(rTable, source).insert()

    protected fun create(vararg sources: D): IntArray = create(sources.toList())

    protected fun create(sources: Collection<D>): IntArray {
        return when {
            sources.isEmpty() -> intArrayOf(0)
            sources.size == 1 -> intArrayOf(create(sources.iterator().next()))
            else              -> sources.map { src -> dsl.newRecord(rTable, src) }.map { r -> r.insert() }.toIntArray()
        }
    }

    protected fun update(vararg sources: D): IntArray = update(sources.toList())

    protected fun update(sources: Collection<D>): IntArray = updateOrDelete(sources, Operation.UPDATE)

    protected fun deleteById(id: ID): Int = dsl.deleteFrom(rTable).where(id.equal()).execute()

    protected fun deleteWhere(condition: () -> Condition): Int = dsl.deleteFrom(rTable).where(condition()).execute()

    protected fun delete(vararg sources: D): IntArray = delete(sources.toList())

    protected fun delete(sources: Collection<D>): IntArray = updateOrDelete(sources, Operation.DELETE)

    protected fun truncate(): Int = dsl.truncate(rTable).restartIdentity().cascade().execute()

    private fun setFieldForUpdateOrDelete(domains: Iterable<D>): List<R> = domains.map {
        val record = dsl.newRecord(rTable, it)
        pkFields.forEach { pkField ->
            record.changed(pkField, false)
        }
        record
    }

    private fun updateOrDelete(sources: Collection<D>, operation: Operation): IntArray {
        return when {
            sources.isEmpty() -> intArrayOf()
            else              -> setFieldForUpdateOrDelete(sources).let {
                when (it.size) {
                    1    ->
                        when (operation) {
                            Operation.DELETE -> intArrayOf(it[0].delete())
                            else             -> intArrayOf(it[0].update())
                        }
                    else -> when (operation) {
                        Operation.DELETE -> it.map { r -> r.delete() }.toIntArray()
                        else             -> it.map { r -> r.update() }.toIntArray()
                    }
                }
            }
        }
    }

    private fun ID.equal(): Condition = when (val id = this) {
        is Identifier -> DSL.row(*pkFields.toTypedArray()).eq(DSL.row(*id.identifier))
        else           -> pkFields[0].eq(pkFields[0].dataType.convert(id))
    }

    private fun <T> Stream<T>.toSequence(): Sequence<T> = Sequence { iterator() }

    private enum class Operation {
        UPDATE, DELETE
    }

}

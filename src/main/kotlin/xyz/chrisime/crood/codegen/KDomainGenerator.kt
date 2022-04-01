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

package xyz.chrisime.crood.codegen

import org.jooq.codegen.GeneratorStrategy.Mode.POJO
import org.jooq.codegen.JavaWriter
import org.jooq.codegen.KotlinGenerator
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.TableDefinition
import org.jooq.tools.JooqLogger
import xyz.chrisime.crood.codegen.CRooDGenerator.Companion.getOptimisticLockMatcher

class KDomainGenerator : CRooDGenerator, KotlinGenerator() {

    override fun generatePojo(table: TableDefinition, out: JavaWriter) {
        generatePojoHeader(table, out)

        val columns = table.columns

        if (columns.size == 1 && columns[0].type.isIdentity) {
            log.info("Table ${table.schema.name}.${table.name} only has one attribute which is a primary key, skipping.")
        } else if (columns.size == 2 && columns[1].type.isNullable) {
            log.info("Second attribute of table ${table.schema.name}.${table.name} is nullable, skipping.")
        } else {
            val pKey = table.primaryKey
            val keyColumnsSize = pKey?.keyColumns?.size ?: 0

            if (keyColumnsSize <= 1) {
                generateConstructor(table, out)
            } else {
                generateCompositeKeyConstructor(table, out)
            }
        }


        val interfaces = out.ref(getStrategy().getJavaClassImplements(table, POJO))

        out.println(")[[before=: ][%s]] {", interfaces)

        if (generatePojosEqualsAndHashCode()) {
            generatePojoEqualsAndHashCode(table, out)
        }

        if (generatePojosToString()) {
            generatePojoToString(table, out)
        }

        out.println().println("}")

        closeJavaWriter(out)
    }

    override fun generateConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        tableDefinition.columns.forEachIndexed { index, column ->
            val separator = if (index + 1 == tableDefinition.columns.size) "" else ", "

            val member = getStrategy().getJavaMemberName(column, POJO)

            if (column is ColumnDefinition) {
                printColumnJPAAnnotation(out, column)
            }

            val versionMatcher = getOptimisticLockMatcher(column.database.recordVersionFields)
            val timestampMatcher = getOptimisticLockMatcher(column.database.recordTimestampFields)
            val columnType = column.getType(resolver(out))

            val transientEnabled = configuration.annotations.useTransient
            if (index == 0) {
                log.info("transient in configuration ${if (transientEnabled) "enabled" else "disabled"}")
            }

            if (
                transientEnabled &&
                (columnType.isIdentity || versionMatcher(column.name) || timestampMatcher(column.name))
            ) {
                out.println("@get:%s", out.ref("java.beans.Transient"))
            }

            generateAnnotations(
                generateValidationAnnotations(),
                out,
                column,
                getJavaType(column.getType(resolver(out)), out),
                resolver(out)
            )

            val nullableStr = if (
                columnType.isIdentity || columnType.isDefaulted ||
                versionMatcher(column.name) || timestampMatcher(column.name)
            ) "? = null" else ""

            out.println(
                "val %s: %s%s%s",
                member,
                out.ref(getJavaType(column.getType(resolver(out, POJO)), out, POJO)),
                nullableStr,
                separator
            )
        }
    }

    override fun generateCompositeKeyConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        TODO("composite key handling not yet implemented")
    }

    override fun printNotNullAnnotation(out: JavaWriter, notNull: String) {
        out.println("@get:%s", out.ref(notNull))
    }

    override fun printSizeAnnotation(out: JavaWriter, sizeAnnotation: String, columnTypeLength: Int) {
        out.println("@get:%s(max = %s)", out.ref(sizeAnnotation), columnTypeLength)
    }

    override fun printPojoHeader(out: JavaWriter, clzName: String, args: List<String>) {
        out.println("data class %s(", clzName)
        out.printSerial()
    }

    private companion object {
        private val log: JooqLogger = JooqLogger.getLogger(KDomainGenerator::class.java)
    }

}

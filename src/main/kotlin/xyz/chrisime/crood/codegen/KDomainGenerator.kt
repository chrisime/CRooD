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
import xyz.chrisime.crood.codegen.CRooDGenerator.Companion.Language.Kotlin
import xyz.chrisime.crood.codegen.CRooDGenerator.Companion.optimisticLockMatcher

class KDomainGenerator : CRooDGenerator, KotlinGenerator() {

    override fun generatePojo(table: TableDefinition, out: JavaWriter) {
        generatePojoHeader(table, out, Kotlin)

        val columns = table.columns

        if (columns.size == 1 && columns[0].type.isIdentity) {
            log.info("Table ${table.name} only has one attribute which is a primary key, skipping.")
        } else if (columns.size == 2 && columns[1].type.isNullable) {
            log.info("Table's second attribute is nullable, skipping.")
        } else {
            val keyColumns = table.primaryKey.keyColumns

            if (keyColumns.size == 1) {
                generateConstructor(table, out)
            } else {
                generateCompositeKeyConstructor(table, out)
            }
        }

        generatePojoFooter(table, out, Kotlin, ::generatePojoEqualsAndHashCode, ::generatePojoToString, ::closeJavaWriter)
    }

    override fun generateConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        tableDefinition.columns.forEachIndexed { i, column ->
            val separator = if (i + 1 == tableDefinition.columns.size) "" else ", "

            val member = getStrategy().getJavaMemberName(column, POJO)

            if (column is ColumnDefinition)
                printColumnJPAAnnotation(out, column)

            val isVersionColumn = optimisticLockMatcher(column.database.recordVersionFields)(column.name)
            val isTstampColumn = optimisticLockMatcher(column.database.recordTimestampFields)(column.name)
            val columnType = column.getType(resolver(out))

            val transientEnabled = serializationConfiguration.annotations.transient
            if (transientEnabled) {
                log.info("transient in configuration enabled")
                if (columnType.isIdentity || isVersionColumn || isTstampColumn) {
                    out.println("@get:%s", out.ref("java.beans.Transient"))
                }
            } else {
                log.info("transient in configuration not enabled")
            }

            generateValidationAnnotations(
                generateValidationAnnotations(),
                Kotlin,
                out,
                column,
                getJavaType(column.getType(resolver(out)), out),
                resolver(out)
            )

            val nullableStr = if (columnType.isIdentity || columnType.isDefaulted || isVersionColumn || isTstampColumn)
                "? = null"
            else
                ""
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

    private companion object {
        private val log: JooqLogger = JooqLogger.getLogger(KDomainGenerator::class.java)
    }

}

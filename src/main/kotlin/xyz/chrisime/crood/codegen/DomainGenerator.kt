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
import org.jooq.codegen.JavaGenerator
import org.jooq.codegen.JavaWriter
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import org.jooq.meta.TypedElementDefinition
import org.jooq.tools.JooqLogger
import xyz.chrisime.crood.codegen.CRooDGenerator.Companion.Language.Java
import xyz.chrisime.crood.codegen.CRooDGenerator.Companion.optimisticLockMatcher

/**
 * Custom generator which additionally creates a secondary constructors, along with nullable annotations if demanded.
 *
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
open class DomainGenerator : CRooDGenerator, JavaGenerator() {

    override fun generatePojo(table: TableDefinition, out: JavaWriter) {
        generatePojoHeader(table, out, Java)

        val columns = table.columns

        generatePojoCopyConstructor(table, out)
        generatePojoMultiConstructor(table, out)

        columns.forEach { column ->
            out.println(
                "private final %s %s;",
                out.ref(getJavaType(column.getType(resolver(out, POJO)), out, POJO)),
                getStrategy().getJavaMemberName(column, POJO)
            )
        }

        columns.forEach {
            generatePojoGetter(it, 0, out)
        }

        generatePojoFooter(table, out, Java, ::generatePojoEqualsAndHashCode, ::generatePojoToString, ::closeJavaWriter)
    }

    override fun generatePojoMultiConstructor(definition: Definition, out: JavaWriter) {
        when (definition) {
            is TableDefinition -> {
                if (definition.primaryKey == null) {
                    log.warn("No primary key found for %s, skipping domain generation.", definition.name)
                    return
                }

                val versionMatcher: (String) -> Boolean = optimisticLockMatcher(definition.database.recordVersionFields)
                val timestampMatcher: (String) -> Boolean = optimisticLockMatcher(definition.database.recordTimestampFields)
                val columns = definition.columns
                val properties = columns.filterNot {
                    versionMatcher(it.name) || timestampMatcher(it.name)
                }.map {
                    "\"${getStrategy().getJavaMemberName(it, POJO)}\""
                }
                out.println("\n    @%s({ [[%s]] })", out.ref("java.beans.ConstructorProperties"), properties)
                if (columns.size == 1 && columns[0].type.isIdentity) {
                    log.info("Table ${definition.name} only has one attribute which is a primary key, skipping.")
                } else if (columns.size == 2 && columns[1].type.isNullable) {
                    log.info("Table's second attribute is nullable, skipping.")
                } else {
                    if (definition.primaryKey.keyColumns.size == 1) {
                        generateConstructor(definition, out)
                    } else {
                        generateCompositeKeyConstructor(definition, out)
                    }
                }
            }
            else -> {
                log.warn("other definition than TableDefinition not allowed")
            }
        }
    }

    override fun generateConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        val keyColumns = tableDefinition.primaryKey.keyColumns
        val versionMatcher: (String) -> Boolean = optimisticLockMatcher(tableDefinition.database.recordVersionFields)
        val timestampMatcher: (String) -> Boolean = optimisticLockMatcher(tableDefinition.database.recordTimestampFields)

        log.info("Generating secondary constructor with primary key ${keyColumns[0].name}.")

        val strategy = getStrategy()

        val ctorArgs = mutableListOf<String>()
        val params = mutableListOf<String>()

        tableDefinition.columns.forEach { column ->
            val version = versionMatcher(column.name)
            val timestamp = timestampMatcher(column.name)

            val javaMemberName = strategy.getJavaMemberName(column, POJO)

            if (version || timestamp) {
                params.add("this.$javaMemberName = null;")
            } else {
                val type = column.getType(resolver(out))
                val fullyQualifiedJavaType = getJavaType(type, out, POJO)
                val javaType = fullyQualifiedJavaType.substringAfterLast(".")

                val annotation = if (generateNullableAnnotation() &&
                    (column.isIdentity || type.isNullable)
                )
                    "@${generatedNullableAnnotationType().substringAfterLast(".")} "
                else if (generateNonnullAnnotation() && !type.isNullable)
                    "@${generatedNonnullAnnotationType().substringAfterLast(".")} "
                else
                    ""

                ctorArgs.add("$annotation$javaType $javaMemberName")
                params.add("this.${javaMemberName} = ${javaMemberName};")
            }
        }

        out.print("public ${strategy.getJavaClassName(tableDefinition, POJO)}(").indentInc()
        out.print(ctorArgs.joinToString(", ")).print(")").indentDec().println(" {")
        out.println(params.joinToString(" ")).println("}")
    }

    override fun generateCompositeKeyConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        TODO("composite key handling not yet implemented")
    }

    override fun generatePojoGetter(column: TypedElementDefinition<*>, index: Int, out: JavaWriter) {
        val isVersionColumn = optimisticLockMatcher(column.database.recordVersionFields)(column.name)
        val isTstampColumn = optimisticLockMatcher(column.database.recordTimestampFields)(column.name)

        out.println()

        if (column.type.isIdentity || isVersionColumn || isTstampColumn) {
            out.println("@%s", out.ref("java.beans.Transient"))
        }

        val strategy = getStrategy()

        if (column is ColumnDefinition) {
            printColumnJPAAnnotation(out, column)
        }

        generateValidationAnnotations(
            generateValidationAnnotations(),
            Java,
            out,
            column,
            getJavaType(column.getType(resolver(out)), out),
            resolver(out)
        )

        if (column.type.isNullable || column.type.isIdentity || column.type.isDefaulted || isVersionColumn || isTstampColumn) {
            printNullableAnnotation(out)
        } else {
            printNonnullAnnotation(out)
        }

        out.overrideIf(generateInterfaces())

        val columnType = out.ref(getJavaType(column.getType(resolver(out, POJO)), out, POJO))
        out.println("public %s %s() {", columnType, strategy.getJavaGetterName(column, POJO))
        out.println("return this.%s;", strategy.getJavaMemberName(column, POJO))
        out.println("}")
    }

    companion object {
        private val log: JooqLogger = JooqLogger.getLogger(DomainGenerator::class.java)
    }

}

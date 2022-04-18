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
import xyz.chrisime.crood.codegen.CRooDGenerator.Companion.getOptimisticLockMatcher

/**
 * Custom generator which additionally creates a secondary constructors, along with nullable annotations if demanded.
 *
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
open class DomainGenerator : CRooDGenerator, JavaGenerator() {

    override fun generatePojo(table: TableDefinition, out: JavaWriter) {
        generatePojoHeader(table, out)

        generatePojoCopyConstructor(table, out)
        generatePojoMultiConstructor(table, out)

        generateClassFields(table, out)

        table.columns.forEach {
            generatePojoGetter(it, 0, out)
        }

        if (generatePojosEqualsAndHashCode()) {
            generatePojoEqualsAndHashCode(table, out)
        }

        if (generatePojosToString()) {
            generatePojoToString(table, out)
        }

        out.println().println("}")

        closeJavaWriter(out)
    }

    override fun generatePojoMultiConstructor(definition: Definition, out: JavaWriter) {
        when (definition) {
            is TableDefinition -> {
                if (definition.primaryKey == null) {
                    log.warn("No primary key found for %s, skipping domain generation.", definition.name)
                    return
                }

                val versionMatcher = getOptimisticLockMatcher(definition.database.recordVersionFields)
                val timestampMatcher = getOptimisticLockMatcher(definition.database.recordTimestampFields)
                val columns = definition.columns

                val properties = columns.filterNot {
                    versionMatcher(it.name) || timestampMatcher(it.name)
                }.map {
                    "\"${getStrategy().getJavaMemberName(it, POJO)}\""
                }
                out.println().println("@%s({ [[%s]] })", out.ref("java.beans.ConstructorProperties"), properties)

                if (columns.size == 1 && columns[0].type.isIdentity) {
                    log.info("Table ${definition.schema.name}.${definition.name} only has one attribute which is a primary key, skipping.")
                } else if (columns.size == 2 && columns[1].type.isNullable) {
                    log.info("Second attribute of table ${definition.schema.name}.${definition.name} is nullable, skipping.")
                } else {
                    if (definition.primaryKey == null || definition.primaryKey.keyColumns.size == 1) {
                        generateConstructor(definition, out)
                    } else {
                        generateCompositeKeyConstructor(definition, out)
                    }
                }
            }
            else -> log.warn("other definition than TableDefinition not allowed")
        }
    }

    override fun generateConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        val keyColumns = tableDefinition.primaryKey.keyColumns
        val versionMatcher = getOptimisticLockMatcher(tableDefinition.database.recordVersionFields)
        val timestampMatcher = getOptimisticLockMatcher(tableDefinition.database.recordTimestampFields)

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

                val annotation = if (generateNullableAnnotation() && (column.isIdentity || type.isNullable))
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
        out.println(params.joinToString("\n")).println("}")
    }

    override fun generateCompositeKeyConstructor(tableDefinition: TableDefinition, out: JavaWriter) {
        TODO("composite key handling not yet implemented")
    }

    private fun generateClassFields(table: TableDefinition, out: JavaWriter) {
        val strategy = getStrategy()

        out.println()

        table.columns.forEach { column ->
            val dataTypeDef = column.getType(resolver(out, POJO))
            val javaType = getJavaType(dataTypeDef, out, POJO)
            out.println("private final %s %s;", out.ref(javaType), strategy.getJavaMemberName(column, POJO))
        }
    }

    override fun generatePojoGetter(column: TypedElementDefinition<*>, index: Int, out: JavaWriter) {
        val versionMatcher = getOptimisticLockMatcher(column.database.recordVersionFields)
        val timestampMatcher = getOptimisticLockMatcher(column.database.recordTimestampFields)

        out.println()

        val transientEnabled = configuration.annotations.useTransient
        if (index == 0) {
            log.info("transient in configuration ${if (transientEnabled) "enabled" else "disabled"}")
        }

        if (transientEnabled &&
            (column.type.isIdentity || versionMatcher(column.name) || timestampMatcher(column.name))
        ) {
            out.println("@%s", out.ref("java.beans.Transient"))
        }

        val strategy = getStrategy()

        if (column is ColumnDefinition) {
            printColumnJPAAnnotation(out, column)
        }

        if (generateValidationAnnotations()) {
            val dataTypeDefinition = column.getType(resolver(out))
            generateAnnotations(out, dataTypeDefinition, getJavaType(dataTypeDefinition, out))
        }

        if (column.type.isNullable || column.type.isIdentity || column.type.isDefaulted ||
            versionMatcher(column.name) || timestampMatcher((column.name))
        ) {
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

    override fun printNotNullAnnotation(out: JavaWriter, notNull: String) {
        out.println("@%s", out.ref(notNull))
    }

    override fun printSizeAnnotation(out: JavaWriter, sizeAnnotation: String, columnTypeLength: Int) {
        out.println("@%s(max = %s)", out.ref(sizeAnnotation), columnTypeLength)
    }

    override fun printPojoHeader(out: JavaWriter, clzName: String, args: List<String>) {
        out.println("public class %s[[before= implements ][%s]] {", clzName, args)
        out.printSerial()
        out.println()
    }

    private companion object {
        private val log: JooqLogger = JooqLogger.getLogger(DomainGenerator::class.java)
    }

}

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

import java.beans.ConstructorProperties
import java.beans.Transient
import org.jooq.codegen.GeneratorStrategy.Mode.POJO
import org.jooq.codegen.JavaGenerator
import org.jooq.codegen.JavaWriter
import org.jooq.impl.SQLDataType
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import org.jooq.meta.TypedElementDefinition
import org.jooq.tools.JooqLogger
import xyz.chrisime.crood.codegen.annotation.ConstructorGenerator

/**
 * Custom generator which additionally creates a secondary constructors, along with nullable annotations if demanded.
 *
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
open class DomainGenerator : ConstructorGenerator, JavaGenerator() {

    init {
        this.setGenerateComments(false)
        this.setGenerateJavadoc(false)
    }

    override fun generatePojoMultiConstructor(definition: Definition, out: JavaWriter) {
        when (definition) {
            is TableDefinition -> {
                if (definition.primaryKey == null) {
                    log.warn("No primary key found for %s, skipping domain generation.", definition.name)
                    return
                }

                generateCtorPropertiesAnnotation(definition, out)
                generateSecondaryConstructors(definition, out)
            }

            else -> {
                log.warn("other definition than TableDefinition not allowed")
            }
        }
    }

    private fun generateCtorPropertiesAnnotation(table: TableDefinition, out: JavaWriter) {
        val versionMatcher: (String) -> Boolean = optLckMatcher(table.database.recordVersionFields)
        val timestampMatcher: (String) -> Boolean = optLckMatcher(table.database.recordTimestampFields)

        val properties = table.columns.filterNot {
            versionMatcher(it.name) || timestampMatcher(it.name)
        }.map {
            "\"${getStrategy().getJavaMemberName(it, POJO)}\""
        }

        out.println("\n    @%s({ [[%s]] })", ConstructorProperties::class.java, properties)
    }

    override fun generateSecondaryConstructors(tableDefinition: TableDefinition, out: JavaWriter) {
        val columns = tableDefinition.columns

        if (columns.size == 1 && columns[0].type.isIdentity) {
            // we don't generate domain objects for tables having only one attribute
            log.info("Table ${tableDefinition.name} only has one attribute which is a primary key, skipping.")
        } else if (columns.size == 2 && columns[1].type.isNullable) {
            log.info("Table's second attribute is nullable, skipping.")
        } else {
            val keyColumns = tableDefinition.primaryKey.keyColumns

            if (keyColumns.size == 1) {
                if (keyColumns[0].type.type in AUTO_INC_PKS) {
                    log.info("Generating secondary constructor, skipping primary key.")

                    val versionMatcher = optLckMatcher(tableDefinition.database.recordVersionFields)
                    val timestampMatcher = optLckMatcher(tableDefinition.database.recordTimestampFields)
                    val strategy = getStrategy()

                    val ctorArgs = mutableListOf<String>()
                    val params = mutableListOf<String>()

                    columns.forEach { column ->
                        val version = versionMatcher(column.name)
                        val timestamp = timestampMatcher(column.name)

                        val javaMemberName = strategy.getJavaMemberName(column, POJO)

                        if (version || timestamp) {
                            params.add("this.$javaMemberName = null;")
                        } else {
                            val fullyQualifiedJavaType = getJavaType(column.type, out, POJO)
                            val javaType = fullyQualifiedJavaType.substringAfterLast(".")

                            val annotation = if (generateNonnullAnnotation() && !column.type.isNullable)
                                "@${generatedNonnullAnnotationType().substringAfterLast(".")} "
                            else if (generateNullableAnnotation() && column.type.isNullable)
                                "@${generatedNullableAnnotationType().substringAfterLast(".")} "
                            else ""

                            ctorArgs.add("$annotation$javaType $javaMemberName")
                            params.add("this.${javaMemberName} = ${javaMemberName};")
                        }
                    }

                    out.print("public ${strategy.getJavaClassName(tableDefinition, POJO)}(").indentInc()
                    out.print(ctorArgs.joinToString(", ")).print(")").indentDec().println(" {")
                    out.println(params.joinToString(" ")).println("}")
                }
            } else {
                TODO("composite key handling not yet implemented")
            }
        }
    }

    override fun generatePojoGetter(column: TypedElementDefinition<*>, index: Int, out: JavaWriter) {
        val isVersionColumn = optLckMatcher(column.database.recordVersionFields)(column.name)
        val isTstampColumn = optLckMatcher(column.database.recordTimestampFields)(column.name)

        if (column.type.isIdentity) {
            out.println("@%s", Transient::class.java)
            super.generatePojoGetter(column, index, out)
        } else if (!isVersionColumn && !isTstampColumn) {
            super.generatePojoGetter(column, index, out)
        }
    }

    companion object {
        private val log: JooqLogger = JooqLogger.getLogger(DomainGenerator::class.java)

        private val AUTO_INC_PKS = setOf(
            SQLDataType.SMALLINT.typeName,
            SQLDataType.INTEGER.typeName,
            SQLDataType.DECIMAL_INTEGER.typeName,
            SQLDataType.BIGINT.typeName
        )

        private val optLckMatcher: (Array<String>) -> (String) -> Boolean = { optimisticFields ->
            { name ->
                if (optimisticFields.isEmpty()) {
                    false
                } else {
                    optimisticFields.any { it.toRegex().matches(name) }
                }
            }
        }
    }

}

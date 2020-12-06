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
import org.jooq.codegen.GeneratorStrategy.Mode.POJO
import org.jooq.codegen.JavaGenerator
import org.jooq.codegen.JavaWriter
import org.jooq.impl.SQLDataType
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import org.jooq.tools.JooqLogger
import xyz.chrisime.crood.codegen.annotation.ConstructorGenerator

/**
 * @author Christian Meyer <christian.meyer@gmail.com>
 */
open class DomainGenerator : ConstructorGenerator, JavaGenerator() {

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
        val properties = table.columns.filterNot {
            it.isIdentity
        }.map {
            "\"${getStrategy().getJavaMemberName(it, POJO)}\""
        }

        out.println("\n    @%s({ [[%s]] })", ConstructorProperties::class.java, properties)
    }


    override fun generateSecondaryConstructors(table: TableDefinition, out: JavaWriter) {
        val columns = table.columns

        if (columns.size == 1 && columns[0].type.isIdentity) {
            // we don't generate domain objects for tables having only one attribute
            log.info("Table ${table.name} only has one attribute which is a primary key, skipping.")
        } else if (columns.size == 2 && columns[1].type.isNullable) {
            log.info("Table's second attribute is nullable, skipping.")
        } else {
            val keyColumns = table.primaryKey.keyColumns

            if (keyColumns.size == 1) {
                val domainName = getStrategy().getJavaClassName(table, POJO)

                if (keyColumns.first().type.type in AUTO_INC_PKS) {
                    log.info("Generating secondary constructor, skipping primary key.")

                    out.print("public $domainName(").indentInc()
                    generate(columns, out)
                }
            } else {
                TODO("composite key handling not yet implemented")
            }
        }
    }

    private fun generate(columns: List<ColumnDefinition>, writer: JavaWriter) {
        val strategy = getStrategy()

        val ctor = mutableListOf<String>()
        val params = mutableListOf<String>()

        columns.forEach { column ->
            val javaMemberName = strategy.getJavaMemberName(column, POJO)

            if (column.primaryKey != null && column.primaryKey.isPrimaryKey) {
                params.add("this.$javaMemberName = null;")
            } else {
                val fullyQualifiedJavaType = getJavaType(column.type, writer, POJO)
                val javaType = fullyQualifiedJavaType.substringAfterLast(".")

                val annotation = if (generateNonnullAnnotation() && !column.type.isNullable)
                    "@${generatedNonnullAnnotationType().substringAfterLast(".")}"
                else if (generateNullableAnnotation() && column.type.isNullable)
                    "@${generatedNullableAnnotationType().substringAfterLast(".")}"
                else ""

                ctor.add("$annotation $javaType $javaMemberName")
                params.add("this.${javaMemberName} = ${javaMemberName};")
            }
        }

        writer.print(ctor.joinToString(", ")).print(")").indentDec().println(" {")
        writer.println(params.joinToString(" ")).println("}")
    }

    companion object {

        @JvmStatic
        private val log: JooqLogger = JooqLogger.getLogger(DomainGenerator::class.java)

        @JvmStatic
        private val AUTO_INC_PKS = setOf(
            SQLDataType.SMALLINT.typeName,
            SQLDataType.INTEGER.typeName,
            SQLDataType.DECIMAL_INTEGER.typeName,
            SQLDataType.BIGINT.typeName
        )
    }

}

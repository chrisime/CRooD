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

package xyz.chrisime.crood.generator

import org.jooq.codegen.GeneratorStrategy.Mode.POJO
import org.jooq.codegen.JavaGenerator
import org.jooq.codegen.JavaWriter
import org.jooq.impl.SQLDataType
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import org.jooq.tools.JooqLogger
import xyz.chrisime.crood.generator.annotation.ConstructorGenerator

/**
 * @author Christian Meyer <christian.meyer@gmail.com>
 */
open class DomainGenerator : ConstructorGenerator, JavaGenerator() {

    private val generateConstructorSkippingPrimaryKey: (ColumnDefinition, StringBuilder, StringBuilder) -> Unit =
        { colDef, ctorBuilder, paramsBuilder ->

            val javaMemberName = getStrategy().getJavaMemberName(colDef, POJO)

            if (colDef.primaryKey != null && colDef.primaryKey.isPrimaryKey) {
                paramsBuilder.append("        this.$javaMemberName = null;\n")
            } else {
                val javaType = getJavaType(colDef.type, POJO)
                ctorBuilder.append("        ${javaType.substringAfterLast(".")} $javaMemberName,\n")
                paramsBuilder.append("        this.${javaMemberName} = ${javaMemberName};\n")
            }
        }

    private val generateConstructorSkippingNulls: (ColumnDefinition, StringBuilder, StringBuilder) -> Unit =
        { colDef, ctorBuilder, paramsBuilder ->

            val javaMemberName = getStrategy().getJavaMemberName(colDef, POJO)

            if (colDef.type.isNullable ||
                colDef.primaryKey != null && colDef.primaryKey.isPrimaryKey && colDef.type.type in AUTO_INC_PKS
            ) {

                paramsBuilder.append("        this.$javaMemberName = null;\n")
            } else {
                val javaType = getJavaType(colDef.type, POJO)
                ctorBuilder.append("        ${javaType.substringAfterLast(".")} $javaMemberName,\n")
                paramsBuilder.append("        this.${javaMemberName} = ${javaMemberName};\n")
            }
        }

    override fun generatePojoMultiConstructor(defintion: Definition, out: JavaWriter) {
        when (defintion) {
            is TableDefinition -> {
                if (defintion.primaryKey == null) {
                    log.warn("No primary key found for %s, skipping domain generation.", defintion.name)
                    return
                }

                super.generatePojoMultiConstructor(defintion, out)

                generateSecondaryConstructors(defintion, out)
            }

            else -> {
                log.warn("other definition than TableDefinition not allowed")
            }
        }
    }

    override fun generateSecondaryConstructors(table: TableDefinition, out: JavaWriter) {
        val columns = table.columns

        if (columns.size == 1) {
            // we don't generate domain objects for tables having only one attribute
            log.info("Table ${table.name} only has one attribute, skipping.")
        } else if (columns.size == 2 && columns[1].type.isNullable) {
            log.info("Table's second attribute is nullable, skipping.")
        } else {
            val keyColumns = table.primaryKey.keyColumns

            if (keyColumns.size == 1) {
                val domainName = getStrategy().getJavaClassName(table, POJO)

                if (keyColumns.first().type.type in AUTO_INC_PKS) {
                    log.info("Generating secondary constructor, skipping primary key.")

                    val ctorArgs = generate(columns, domainName, generateConstructorSkippingPrimaryKey)
                    out.println().tab(1).println(ctorArgs)
                }

                val nullable = columns.filter {
                    it.primaryKey == null
                }.fold(false) { acc, cur ->
                    acc || cur.type.isNullable
                }

                if (nullable && columns.size > 2) {
                    log.info("Generating secondary constructor, skipping nullable arguments.")

                    val ctorArgs = generate(columns, domainName, generateConstructorSkippingNulls)
                    out.println().tab(1).println(ctorArgs)
                }
            } else {
                TODO("composite key handling not yet implemented")
            }
        }
    }

    private fun generate(
        columns: List<ColumnDefinition>, domainName: String,
        fn: (ColumnDefinition, StringBuilder, StringBuilder) -> Unit
    ): String {
        val ctorBuilder = StringBuilder("public ${domainName}(\n")
        val paramsBuilder = StringBuilder()

        columns.forEach { column ->
            fn(column, ctorBuilder, paramsBuilder)
        }

        ctorBuilder.replace(ctorBuilder.length - 2, ctorBuilder.length, "")

        return ctorBuilder.append("\n    ) {\n").append(paramsBuilder).append("\n    }").toString()
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

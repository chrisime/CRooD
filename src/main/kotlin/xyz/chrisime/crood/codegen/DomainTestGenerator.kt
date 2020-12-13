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

import org.jooq.codegen.GeneratorStrategy
import org.jooq.codegen.GeneratorStrategy.Mode.POJO
import org.jooq.codegen.JavaGenerator
import org.jooq.codegen.JavaWriter
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.TableDefinition
import xyz.chrisime.crood.codegen.annotation.ConstructorGenerator
import xyz.chrisime.crood.codegen.annotation.ForeignKey
import xyz.chrisime.crood.codegen.annotation.Size

/**
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
class DomainTestGenerator : ConstructorGenerator, JavaGenerator() {

    override fun generatePojo(table: TableDefinition, out: JavaWriter) {
        val strategy = super.getStrategy()
        val className = strategy.getJavaClassName(table, POJO)
        val superName = out.ref(strategy.getJavaClassExtends(table, POJO))
        val interfaces = out.ref(strategy.getJavaClassImplements(table, POJO))

        super.printPackage(out, table, POJO)

        val columns = table.columns

        out.println("public class ${className}[[before= extends ][${superName}]][[before= implements ][${interfaces}]] {").println()

        createDefaultConstructor(out, className, columns)

        columns.forEach { column ->
            val type = column.type
            val len = type.length
            val annotation = if ("java.lang.String" == getJavaType(type, out) && len > 0)
                "@${Size::class.simpleName}(max = ${len})"
            else
                ""

            out.tab(1)
                .println(annotation)
                .println("private ${out.ref(getJavaType(column.type, out))} ${strategy.getJavaMemberName(column, POJO)};")
        }

        (0..columns.size).forEach { i ->
            val column = columns[i]
            generatePojoGetter(column, i, out)

            if (column.foreignKeys.isEmpty())
                generatePojoSetter(column, i, out)
        }

        generateForeignKeyFields(table, out, strategy)

        generatePojoEqualsAndHashCode(table, out)

        out.println("}")
        closeJavaWriter(out)
    }

    private fun createDefaultConstructor(out: JavaWriter, className: String, columns: List<ColumnDefinition>) {
        out.tab(1).println("public %s() {", className).tab(2).print("super(")

        columns.map { columnDef ->
            if (columnDef.position == 1) "null" else ", null"
        }.forEach { nullArg ->
            out.tab(2).print(nullArg)
        }

        out.tab(2).println(");").tab(1).println("}").println()
    }

    private fun generateForeignKeyFields(table: TableDefinition, out: JavaWriter, strategy: GeneratorStrategy) {
        table.foreignKeys.forEach { fkDef ->
            val referencedTable = fkDef.referencedTable
            val keyColumns = fkDef.keyColumns

            // we don't have composed foreign keys for now, so just get the first one in the list
            if (keyColumns.size == 1) {
                val refColumnDefinition = fkDef.referencedColumns[0]
                val refColumnName = strategy.getJavaMemberName(refColumnDefinition, POJO)
                val clsName = strategy.getFullJavaClassName(referencedTable, POJO)
                val memberName = strategy.getJavaMemberName(referencedTable, POJO)
                // in some tables the foreign key name doesn't match the table name
                val fkColumnName = strategy.getJavaMemberName(keyColumns[0], POJO)
                val cleansedMemberName = memberName.replace("_", "")
                val getterName = strategy.getJavaGetterName(referencedTable, POJO)
                val cleansedGetterName = getterName.replace("_", "")
                val setterName = strategy.getJavaSetterName(referencedTable, POJO)
                val cleansedSetterName = setterName.replace("_", "")

                out.println().tab(1)
                out.tab(1)
                    .println("@${ForeignKey::class.simpleName}(refColumnName=\"${refColumnName}\", columnName=\"${fkColumnName}\", isNullable=${keyColumns[0].type.isNullable})")
                out.tab(1)
                    .println("private $clsName ${cleansedMemberName}Domain;")
                out.tab(1)
                    .println("public $clsName ${cleansedGetterName}Domain() { return ${cleansedMemberName}Domain; }")
                out.tab(1)
                    .println("public void ${cleansedSetterName}Domain(${clsName} ${cleansedMemberName}Domain) { this.${cleansedMemberName}Domain = ${cleansedMemberName}Domain; }")
                out.println().tab(1)
            } else {
                TODO("composite key handling not yet implemented")
            }
        }
    }

}

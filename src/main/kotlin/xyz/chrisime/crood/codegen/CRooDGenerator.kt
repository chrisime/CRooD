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

import org.jooq.codegen.Generator
import org.jooq.codegen.GeneratorStrategy.Mode
import org.jooq.codegen.JavaWriter
import org.jooq.meta.DataTypeDefinition
import org.jooq.meta.Definition
import org.jooq.meta.JavaTypeResolver
import org.jooq.meta.TableDefinition
import org.jooq.meta.TypedElementDefinition
import xyz.chrisime.crood.config.CRooDConfig
import xyz.chrisime.crood.config.CRooDConfigurationLoader

internal interface CRooDGenerator : Generator {

    val configuration: CRooDConfig
        get() = CRooDConfigurationLoader.croodConfigOfUserDir

    val jakartaValidation: String
        get() = "jakarta.validation.constraints"

    val javaxValidation: String
        get() = "javax.validation.constraints"

    fun generateConstructor(tableDefinition: TableDefinition, out: JavaWriter)

    fun generateCompositeKeyConstructor(tableDefinition: TableDefinition, out: JavaWriter)

    fun printNotNullAnnotation(out: JavaWriter, notNull: String)

    fun printSizeAnnotation(out: JavaWriter, sizeAnnotation: String, columnTypeLength: Int)

    fun printPojoHeader(out: JavaWriter, clzName: String, args: List<String>)

    fun generatePackage(out: JavaWriter, definition: Definition, mode: Mode) {
        out.printPackageSpecification(strategy.getJavaPackageName(definition, mode))
        out.printImports()
    }

    fun generateAnnotations(out: JavaWriter, columnType: DataTypeDefinition, javaType: String) {
        if (!columnType.isNullable && !columnType.isDefaulted && !columnType.isIdentity) {
            val notNull = if (configuration.annotations.useJakarta) {
                "${jakartaValidation}.NotNull"
            } else {
                "${javaxValidation}.NotNull"
            }

            printNotNullAnnotation(out, notNull)
        }

        if (javaType in setOf("java.lang.String", "byte[]")) {
            val length = columnType.length
            if (length > 0) {
                val sizeAnnotation = if (configuration.annotations.useJakarta) {
                    "${jakartaValidation}.Size"
                } else {
                    "${javaxValidation}.Size"
                }

                printSizeAnnotation(out, sizeAnnotation, length)
            }
        }
    }

    fun generatePojoHeader(table: TableDefinition, out: JavaWriter) {
        val className = strategy.getJavaClassName(table, Mode.POJO)
        val interfaces = out.ref(strategy.getJavaClassImplements(table, Mode.POJO))

        generatePackage(out, table, Mode.POJO)

        out.javadoc(strategy.getFileHeader(table, Mode.POJO))

        if (configuration.frameworks.isMicronaut) {
            out.println("@%s", out.ref("io.micronaut.core.annotation.Introspected"))
        }

        printPojoHeader(out, className, interfaces)
    }

    companion object {
        internal fun getOptimisticLockMatcher(optimisticFields: Array<String>): (String) -> Boolean = { name ->
            if (optimisticFields.isEmpty()) {
                false
            } else {
                optimisticFields.any { it.toRegex().matches(name) }
            }
        }
    }

}

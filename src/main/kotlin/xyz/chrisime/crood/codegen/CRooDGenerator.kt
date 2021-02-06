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
import org.jooq.meta.Definition
import org.jooq.meta.JavaTypeResolver
import org.jooq.meta.TableDefinition
import org.jooq.meta.TypedElementDefinition
import xyz.chrisime.crood.config.CRooDYaml
import xyz.chrisime.crood.config.croodConfig

internal interface CRooDGenerator : Generator {

    val serializationConfiguration: CRooDYaml.Serialization
        get() = croodConfig.serialization

    fun generateConstructor(tableDefinition: TableDefinition, out: JavaWriter)

    fun generateCompositeKeyConstructor(tableDefinition: TableDefinition, out: JavaWriter)

    fun generatePackage(out: JavaWriter, definition: Definition, mode: Mode) {
        out.printPackageSpecification(strategy.getJavaPackageName(definition, mode))
        out.printImports()
    }

    fun generateValidationAnnotations(
        enabled: Boolean,
        language: Language,
        out: JavaWriter,
        column: TypedElementDefinition<*>,
        javaType: String,
        javaTypeResolver: JavaTypeResolver
    ) {
        if (!enabled)
            return

        val columnType = column.getType(javaTypeResolver)

        if (!columnType.isNullable && !columnType.isDefaulted && !columnType.isIdentity) {
            if (language == Language.Java)
                out.println("@%s", out.ref("javax.validation.constraints.NotNull"))
            else
                out.println("@get:%s", out.ref("javax.validation.constraints.NotNull"))
        }

        if ("java.lang.String" == javaType || "byte[]" == javaType) {
            val length = columnType.length
            if (length > 0)
                if (language == Language.Java)
                    out.println("@%s(max = %s)", out.ref("javax.validation.constraints.Size"), length)
                else
                    out.println("@get:%s(max = %s)", out.ref("javax.validation.constraints.Size"), length)
        }
    }

    fun generatePojoHeader(table: TableDefinition, out: JavaWriter, language: Language) {
        val className = strategy.getJavaClassName(table, Mode.POJO)
        val interfaces = out.ref(strategy.getJavaClassImplements(table, Mode.POJO))

        generatePackage(out, table, Mode.POJO)

        out.javadoc(strategy.getFileHeader(table, Mode.POJO))

        if (language == Language.Java)
            out.println("public class %s[[before= implements ][%s]] {", className, interfaces)
        else
            out.println("data class %s(", className)

        out.printSerial()

        if (language == Language.Java) {
            out.println()
        }
    }

    fun generatePojoFooter(
        table: TableDefinition,
        out: JavaWriter,
        language: Language,
        equalsAndHashCodeFn: (TableDefinition, JavaWriter) -> Unit,
        toStringFn: (TableDefinition, JavaWriter) -> Unit,
        javaWriterFn: (JavaWriter) -> Unit
    ) {

        if (language == Language.Java) {
            if (generatePojosEqualsAndHashCode()) equalsAndHashCodeFn(table, out)

            if (generatePojosToString()) toStringFn(table, out)

        } else {
            val interfaces = out.ref(strategy.getJavaClassImplements(table, Mode.POJO))

            out.println(")[[before=: ][%s]] {", interfaces)

            if (generatePojosEqualsAndHashCode()) equalsAndHashCodeFn(table, out)

            if (generatePojosToString()) toStringFn(table, out)
        }

        out.println().println("}")
        javaWriterFn(out)
    }

    companion object {
        internal enum class Language {
            Java, Kotlin
        }

        internal val optimisticLockMatcher: (Array<String>) -> (String) -> Boolean = { optimisticFields ->
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

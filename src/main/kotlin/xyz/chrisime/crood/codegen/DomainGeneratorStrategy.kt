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

import xyz.chrisime.crood.domain.IdentifiableDomain
import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GeneratorStrategy
import org.jooq.meta.Definition

/**
 * Custom strategy that defines package structure, POJO name and a common interface which all POJOs implement.
 *
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
open class DomainGeneratorStrategy : DefaultGeneratorStrategy() {

    override fun getJavaClassImplements(definition: Definition, mode: GeneratorStrategy.Mode): List<String> {
        return if (mode == GeneratorStrategy.Mode.POJO)
            listOf(IdentifiableDomain::class.java.name)
        else
            super.getJavaClassImplements(definition, mode)
    }

    override fun getJavaPackageName(definition: Definition, mode: GeneratorStrategy.Mode): String {
        val sb = StringBuilder()

        if (mode == GeneratorStrategy.Mode.POJO) {
            sb.append(targetPackage)

            if (definition.database.schemata.size > 1)
                sb.append(".").append(getJavaIdentifier(definition.schema).toLowerCase())

            sb.append(".domain")
        } else {
            sb.append(super.getJavaPackageName(definition, mode))
        }

        return sb.toString()
    }

    override fun getJavaClassName(definition: Definition, mode: GeneratorStrategy.Mode?): String {
        val javaClassName = super.getJavaClassName(definition, mode)

        return if (mode == GeneratorStrategy.Mode.POJO) "$javaClassName$DOMAIN" else javaClassName
    }

    internal companion object {
        internal const val DOMAIN = "Domain"
    }

}

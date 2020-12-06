package xyz.chrisime.crood.codegen

import xyz.chrisime.crood.domain.IdentifiableDomain
import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GeneratorStrategy
import org.jooq.meta.Definition

/**
 * @author Christian Meyer <christian.meyer@gmail.com>
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

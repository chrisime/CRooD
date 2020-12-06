package xyz.chrisime.crood.codegen

import org.jooq.codegen.GeneratorStrategy
import org.jooq.meta.Definition

class DomainTestGeneratorStrategy : DomainGeneratorStrategy() {

    override fun getJavaClassExtends(definition: Definition, mode: GeneratorStrategy.Mode?): String {
        val clsName = super.getJavaClassName(definition, mode)
        return if (mode == GeneratorStrategy.Mode.POJO) clsName.replace("Record".toRegex(),
                                                                        DOMAIN
        ) else clsName
    }

    override fun getJavaClassName(definition: Definition, mode: GeneratorStrategy.Mode?): String {
        val javaClassName = super.getJavaClassName(definition, mode)
        return if (mode == GeneratorStrategy.Mode.POJO) javaClassName.replace(
            DOMAIN,
                                                                              DOMAIN_POSTFIX
        ) else javaClassName
    }

    private companion object {
        private const val DOMAIN_POSTFIX = "TestDomain"
    }

}

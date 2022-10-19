package xyz.chrisime.micronaut

import io.micronaut.configuration.jooq.DSLContextFactory
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.runtime.Micronaut
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.conf.RenderQuotedNames
import org.jooq.impl.DefaultDSLContext

fun main(args: Array<String>) {
    Micronaut.build(*args).packages("xyz.chrisime").banner(false).start()
}

@Factory
@Replaces(factory = DSLContextFactory::class)
class DSLConfigurationFactory {
    @EachBean(Configuration::class)
    fun dslContext(configuration: Configuration): DSLContext {
        val config = configuration.apply {
            with(settings()) {
                withExecuteWithOptimisticLocking(true)
                withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED)
            }
        }
        return DefaultDSLContext(config)
    }
}

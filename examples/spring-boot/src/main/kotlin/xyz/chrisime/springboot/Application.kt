package xyz.chrisime.springboot

import org.jooq.conf.RenderQuotedNames
import org.jooq.impl.DefaultConfiguration
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@SpringBootConfiguration
@EnableTransactionManagement
class Config {
    @Bean
    fun configurationCustomizer(): DefaultConfigurationCustomizer {
        return DefaultConfigurationCustomizer { c: DefaultConfiguration ->
            with(c.settings()) {
                withExecuteWithOptimisticLocking(true)
                withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED)
            }
        }
    }
}

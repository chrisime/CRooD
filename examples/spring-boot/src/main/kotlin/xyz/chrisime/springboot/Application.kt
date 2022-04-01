package xyz.chrisime.springboot

import org.jooq.conf.RenderQuotedNames
import org.jooq.impl.DefaultConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement


@SpringBootApplication
@EnableTransactionManagement
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Configuration
class Config {
    @Bean
    fun configurationCustomizer(): DefaultConfigurationCustomizer {
        return DefaultConfigurationCustomizer { c: DefaultConfiguration ->
            c.settings().withExecuteWithOptimisticLocking(true)
            c.settings().withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED)
        }
    }
}

package xyz.chrisime.crood.codegen.annotation

import org.jooq.codegen.JavaWriter
import org.jooq.meta.TableDefinition

interface ConstructorGenerator {

    fun generateSecondaryConstructors(tableDefinition: TableDefinition, out: JavaWriter) {
        // by default do nothing
    }

}

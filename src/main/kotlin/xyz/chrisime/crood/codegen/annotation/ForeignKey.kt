package xyz.chrisime.crood.codegen.annotation

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ForeignKey(val refColumnName: String = "id", val columnName: String, val isNullable: Boolean = false)

package xyz.chrisime.crood.generator.annotation

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ForeignKey(val refColumnName: String = "id", val columnName: String, val isNullable: Boolean = false)

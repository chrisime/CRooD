package xyz.chrisime.crood.generator.annotation

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Size(val min: Int = 0, val max: Int = Integer.MAX_VALUE)

package xyz.chrisime.crood.extensions

inline fun <reified T : Any> Any?.asType(): T = this as T

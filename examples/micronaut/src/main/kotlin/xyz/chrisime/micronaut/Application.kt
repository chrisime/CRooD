package xyz.chrisime.micronaut

import io.micronaut.runtime.Micronaut

fun main(args: Array<String>) {
    Micronaut.build(*args).packages("xyz.chrisime").start()
}

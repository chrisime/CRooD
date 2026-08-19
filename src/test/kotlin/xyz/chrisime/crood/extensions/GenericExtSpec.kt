package xyz.chrisime.crood.extensions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

open class Base<A, B>
class Foo : Base<String, Integer>()

open class Root<X>
open class Mid<A> : Root<Long>()
class ChainedFoo : Mid<String>()

open class SingleArgBase<A>
class StringFoo : SingleArgBase<String>()

class IntHolder(val value: Int)
open class NoArgBase<A>
class NoArgCtorFoo : NoArgBase<IntHolder>()

class GenericExtSpec : BehaviorSpec(
    {

        given("an instance of a directly parameterized superclass") {
            `when`("resolving the type argument at index 0") {
                val result = Foo().getClassAtIndex<Class<*>>(0)

                then("it returns the String type") {
                    result shouldBe String::class.java
                }
            }

            `when`("resolving the type argument at index 1") {
                val result = Foo().getClassAtIndex<Class<*>>(1)

                then("it returns the Integer type") {
                    result shouldBe Integer::class.java
                }
            }
        }

        given("an instance of a class with a chained generic superclass") {
            `when`("resolving the type argument at index 0") {
                val result = ChainedFoo().getClassAtIndex<Class<*>>(0)

                then("it returns the String type") {
                    result shouldBe String::class.java
                }
            }
        }

        given("an instance of a class whose superclass is not parameterized") {
            `when`("resolving any type argument") {
                then("it throws a RuntimeException") {
                    shouldThrow<RuntimeException> {
                        Any().getClassAtIndex<Class<*>>(0)
                    }
                }
            }
        }

        given("an instance of a class with a single generic type argument") {
            `when`("resolving an out-of-bounds index") {
                then("it throws a RuntimeException") {
                    shouldThrow<RuntimeException> {
                        StringFoo().getClassAtIndex<Class<*>>(2)
                    }
                }
            }
        }

        given("an instance of a directly parameterized superclass with a no-arg constructor") {
            `when`("instantiating the resolved type argument") {
                val result = StringFoo().newInstance<Any>()

                then("it returns a non-null String instance") {
                    result shouldBe ""
                }
            }
        }

        given("an instance whose resolved type argument has no no-arg constructor") {
            `when`("instantiating the resolved type argument") {
                then("it throws a RuntimeException") {
                    shouldThrow<RuntimeException> {
                        NoArgCtorFoo().newInstance<Any>()
                    }
                }
            }
        }
    }
)
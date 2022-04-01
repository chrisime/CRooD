package xyz.chrisime.crood.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import xyz.chrisime.crood.config.CRooDConfigurationLoader.defaultConfiguration

class CRooDConfigSpec : BehaviorSpec(
    {

        given("crood configuration file") {
            val configurationFile = "src/test/resources/crood-test.json"

            `when`("a configuration is be read") {
                val result = CRooDConfigurationLoader.readConfiguration(configurationFile)

                then("then there should be the expected results") {
                    result shouldBe CRooDConfig(
                        Annotations(useTransient = true, useJakarta = true),
                        Frameworks(isMicronaut = true)
                    )
                }
            }
        }

        given("configuration file that doesn't exist") {
            val configurationFile = "this/does not/exist.json"

            `when`("a configuration is be read") {
                val result = CRooDConfigurationLoader.readConfiguration(configurationFile)

                then("then it returns the default configuration") {
                    result shouldBe defaultConfiguration
                }
            }
        }
    }
)

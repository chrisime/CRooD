package xyz.chrisime.crood.config

import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

object CRooDConfigurationLoader {
    val croodConfigOfUserDir: CRooDConfig = readConfiguration("${System.getenv("HOME")}/crood.json")

    val defaultConfiguration = CRooDConfig(
        annotations = Annotations(useJakarta = false, useTransient = false),
        frameworks = Frameworks(isMicronaut = false)
    )

    fun readConfiguration(configFile: String): CRooDConfig {
        val file = File(configFile)

        return if (!file.exists() || !file.isFile || !file.canRead()) {
            defaultConfiguration
        } else {
            file.reader()
                .use {
                    val tokenizer = JSONTokener(it)
                    val jsonObject = JSONObject(tokenizer)

                    if (jsonObject.isEmpty) {
                        defaultConfiguration
                    } else {
                        val annotations = if (!jsonObject.keySet().contains("annotations")) {
                            Annotations(useTransient = false, useJakarta = false)
                        } else {
                            val serialization = jsonObject.getJSONObject("annotations")
                            Annotations(
                                serialization.optBoolean("enableTransient", false),
                                serialization.optBoolean("enableJakarta", false)
                            )
                        }

                        val isMicronaut = if (jsonObject.keySet().contains("frameworks")) {
                            jsonObject.getJSONObject("frameworks").optBoolean("isMicronaut", false)
                        } else {
                            false
                        }

                        CRooDConfig(annotations, Frameworks(isMicronaut = isMicronaut))
                    }
                }
        }
    }

}

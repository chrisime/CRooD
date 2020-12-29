package xyz.chrisime.crood.config

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.introspector.BeanAccess
import java.io.File

object Configuration {
    private val ymlFile: File by lazy { File("src/main/resources/crood.yml") }
    private val yamlFile: File by lazy { File("src/main/resources/crood.yaml") }

    fun getConfig(): CRooDYaml {
        return ymlFile.parseYaml<CRooDYaml>() ?: yamlFile.parseYaml<CRooDYaml>() ?: throw IllegalArgumentException()
    }
}

inline fun <reified T> File.parseYaml(): T? {
    val inputStream = ClassLoader.getSystemResourceAsStream(name) ?: return null

    return inputStream.bufferedReader().use { bis ->
        with(Yaml(Constructor(T::class.java))) {
            setBeanAccess(BeanAccess.FIELD)
            load(bis)
        }
    }
}


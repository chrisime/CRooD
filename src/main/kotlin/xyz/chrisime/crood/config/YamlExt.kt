package xyz.chrisime.crood.config

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.introspector.BeanAccess
import java.io.File

private val configFiles: List<File> by lazy {
    listOf(
        File("src/main/resources/crood.yml"),
        File("src/main/resources/crood.yaml")
    )
}

val croodConfig = configFiles.map {
    it.parseYaml<CRooDYaml>()
}.first {
    it != null
} ?: throw IllegalArgumentException()

private inline fun <reified T> File.parseYaml(): T? {
    val inputStream = ClassLoader.getSystemResourceAsStream(name) ?: return null

    return inputStream.bufferedReader().use { bis ->
        with(Yaml(Constructor(T::class.java))) {
            setBeanAccess(BeanAccess.FIELD)
            load(bis)
        }
    }
}

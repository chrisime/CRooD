package xyz.chrisime.crood.config

import com.charleskorn.kaml.Yaml
import java.io.File

private val configFiles: List<File> by lazy {
    listOf(
        File("src/main/resources/crood.yml"),
        File("src/main/resources/crood.yaml")
    )
}

val croodConfig = configFiles.map {
    it.parseYaml()
}.first {
    it != null
} ?: throw IllegalArgumentException()

private fun File.parseYaml(): CRooDYaml? {
    val inputStream = ClassLoader.getSystemResourceAsStream(name) ?: return null
    val yaml = Yaml.default

    return inputStream.bufferedReader().use { bis ->
        val text = bis.readText()
        yaml.decodeFromString(CRooDYaml.serializer(), text)
    }
}

rootProject.name = "CRooD"

pluginManagement {
    val kotlinVersion: String by settings
    val dependenciesVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.github.ben-manes.versions") version dependenciesVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }

    val jooqVersion: String by settings
    val junit: String by settings
    val kotlinVersion: String by settings
    val kotestVersion: String by settings
    versionCatalogs {
        create("libs") {
            library("jooq-parent", "org.jooq", "jooq-parent").version(jooqVersion)
            library("jooq-codegen", "org.jooq", "jooq-codegen").version(jooqVersion)
            library("jooq-meta", "org.jooq", "jooq-meta").version(jooqVersion)
            library("jooq", "org.jooq", "jooq").version(jooqVersion)
            bundle("jooq", listOf("jooq-codegen", "jooq-meta"))

            library("kotlin-bom", "org.jetbrains.kotlin", "kotlin-bom").version(kotlinVersion)
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").version(kotlinVersion)

            library("json", "org.json", "json").version("20220320")

            library("junit", "org.junit", "junit-bom").version(junit)
            library("kotest-junit5", "io.kotest", "kotest-runner-junit5-jvm").version(kotestVersion)
            library("kotest-assertions", "io.kotest", "kotest-assertions-core-jvm").version(kotestVersion)
            bundle("kotest", listOf("kotest-junit5", "kotest-assertions"))
        }
    }
}

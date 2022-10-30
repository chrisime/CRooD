plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("kapt") version "1.7.20"
    kotlin("plugin.allopen") version "1.7.20"

    id("io.micronaut.minimal.application") version "3.6.3"

    id("nu.studer.jooq") version "7.1.1"

    id("com.github.ben-manes.versions") version "0.42.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

description = "Micronaut CRooD Example"
group = "xyz.chrisime"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(platform("io.micronaut:micronaut-bom:3.7.3"))
    kapt(platform("io.micronaut:micronaut-bom:3.7.3"))

    implementation("io.micronaut", "micronaut-management")
    implementation("io.micronaut", "micronaut-validation")
    implementation("io.micronaut.kotlin", "micronaut-kotlin-runtime")
    implementation("io.micronaut.data", "micronaut-data-tx", "3.8.1")
    implementation("io.micronaut.flyway", "micronaut-flyway", "5.4.1")
    implementation("io.micronaut.sql", "micronaut-jooq", "4.7.2")

    implementation("xyz.chrisime", "crood", "0.3.0+")

    runtimeOnly("io.micronaut.sql", "micronaut-jdbc-hikari", "4.7.2")
    runtimeOnly("org.postgresql", "postgresql", "42.5.0")
    runtimeOnly("org.jetbrains.kotlin", "kotlin-reflect", "1.7.20")

    compileOnly("jakarta.validation", "jakarta.validation-api", "3.0.2")

    jooqGenerator(project(":generator"))
    jooqGenerator("xyz.chrisime", "crood", "0.3.0+")
}

configurations {
    all {
        resolutionStrategy.eachDependency {
            if (requested.name == "reactor-core") {
                useVersion("3.4.24")
            }
        }
    }
}

tasks {
    allOpen {
        annotations("javax.transaction.Transactional")
    }

    kapt {
        useBuildCache = false
        keepJavacAnnotationProcessors = true
        showProcessorStats = true
    }

    micronaut {
        enableNativeImage(false)
            .version("3.4.2")
            .runtime(io.micronaut.gradle.MicronautRuntime.NETTY)
            .processing {
                incremental(true)
                    .module(project.name)
                    .group("${project.group}")
                    .annotations("xyz.chrisime.micronaut.*", "javax.transaction.Transactional")
            }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    compileKotlin {
        dependsOn("generateJooq")

        kotlinOptions {
            jvmTarget = "${JavaVersion.VERSION_11}"
            apiVersion = "1.6"
            languageVersion = "1.6"

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    application {
        mainClass.set("xyz.chrisime.micronaut.ApplicationKt")
    }

    jooq {
        version.set("3.17.4")
        configurations {
            create("main") {
                generateSchemaSourceOnCompilation.set(false)
                jooqConfiguration.apply {
                    withLogging(org.jooq.meta.jaxb.Logging.WARN).withGenerator(
                        generator.apply {
                            name = "xyz.chrisime.crood.codegen.KDomainGenerator"
                            strategy.apply {
                                name = "xyz.chrisime.crood.codegen.DomainGeneratorStrategy"
                            }
                            database.apply {
                                withName("xyz.chrisime.jooq.generator.StandalonePostgresDatabase")
                                    .withInputSchema("public")
                                    .withRecordVersionFields("version")
                                    .withExcludes("flyway_.*")
                            }
                            generate.apply {
                                withValidationAnnotations(true)
                                    .withDeprecated(false)
                                    .withRecords(true)
                                    .withImmutablePojos(true)
                                    .withRoutines(true)
                                    .withGlobalObjectReferences(true)
                                    .withGlobalKeyReferences(true)
                            }
                            target.apply {
                                withPackageName("xyz.chrisime.micronaut")
                                    .withDirectory("${project.buildDir}/generated/jooq")
                            }
                        }
                    )
                }
            }
        }
    }
}

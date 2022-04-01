plugins {
    id("org.springframework.boot") version "2.6.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"

    id("nu.studer.jooq") version "7.1.1"
}

group = "xyz.chrisime"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-jooq")
    implementation("org.springframework.boot", "spring-boot-starter-webflux")

    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin")

    implementation("org.flywaydb", "flyway-core")

    implementation("xyz.chrisime", "crood", "0.3.0+")

    runtimeOnly("org.postgresql", "postgresql")
    runtimeOnly("com.zaxxer", "HikariCP")

    compileOnly("jakarta.validation", "jakarta.validation-api", "3.0.0")
    implementation("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.0")

    jooqGenerator(project(":generator"))
    jooqGenerator("xyz.chrisime", "crood", "0.3.0+")
    jooqGenerator("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.0")
}

tasks {
    compileKotlin {
        dependsOn("generateJooq")

        kotlinOptions {
            jvmTarget = sourceCompatibility
            apiVersion = "1.6"
            languageVersion = "1.6"

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xstrict-java-nullability-assertions",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    jooq {
        version.set("3.16.5")
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
                                withPackageName("xyz.chrisime.springboot")
                                    .withDirectory("${project.buildDir}/generated/jooq")
                            }
                        }
                    )
                }
            }
        }
    }
}

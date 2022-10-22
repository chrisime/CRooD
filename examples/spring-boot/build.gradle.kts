plugins {
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"

    kotlin("jvm") version "1.7.20"
    kotlin("plugin.spring") version "1.7.20"

    id("nu.studer.jooq") version "7.1.1"

    id("com.github.ben-manes.versions") version "0.42.0"
}

description = "Spring Boot CRooD Example"
group = "xyz.chrisime"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencyManagement {
    dependencies {
        dependency("org.yaml:snakeyaml:1.33")
    }
}

dependencies {
    implementation("org.springframework.boot", "spring-boot-starter-jdbc")
    implementation("org.springframework.boot", "spring-boot-starter-json")
    implementation("org.springframework.boot", "spring-boot-starter-webflux")

    implementation("org.flywaydb", "flyway-core")

    implementation("xyz.chrisime", "crood", "0.3.0+")

    implementation("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.0")

    runtimeOnly("org.postgresql", "postgresql", "42.5.0")

    compileOnly("jakarta.validation", "jakarta.validation-api", "3.0.2")

    jooqGenerator(project(":generator"))
    jooqGenerator("xyz.chrisime", "crood", "0.3.0+")
    jooqGenerator("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.0")
}

tasks {
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

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

plugins {
    kotlin("jvm") version "1.4.20"
    `java-library`

    id("nu.studer.jooq") version "5.2"// apply false

    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"

    id("com.github.ben-manes.versions") version "0.36.0"
}

version = "0.1-SNAPSHOT"
group = "xyz.chrisime"

val jooqVersion = "3.14.+"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation(enforcedPlatform("org.jooq:jooq-parent:${jooqVersion}"))

    implementation("org.jooq:jooq")
//    implementation("org.jooq:jooq-kotlin")

    compileOnly("org.jooq:jooq-codegen")
    compileOnly("org.jooq:jooq-meta")

    testImplementation("org.jetbrains.kotlin:kotlin-test") {
        isTransitive = false
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit") {
        isTransitive = false
    }
}

//configurations {
//    compileClasspath {
//        resolutionStrategy.eachDependency {
//            if (requested.group == "org.jooq") {
//                useVersion("3.14.+")
//                useTarget("org.jooq:jooq:3.14.+")
//                useTarget("org.jooq:jooq-kotlin:3.14.+")
//            }
//        }
//    }
//}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jooq:jooq-kotlin"))
            .using(module("org.jooq:jooq-kotlin:${jooqVersion}"))
            .withoutClassifier()

        substitute(module("org.jooq:jooq"))
            .using(module("org.jooq:jooq:${jooqVersion}"))
            .withoutClassifier()

        substitute(module("org.jooq:jooq-codegen"))
            .using(module("org.jooq:jooq-codegen:${jooqVersion}"))
            .withoutClassifier()

        substitute(module("org.jooq:jooq-meta"))
            .using(module("org.jooq:jooq-meta:${jooqVersion}"))
            .withoutClassifier()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = sourceCompatibility

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xstrict-java-nullability-assertions"
            )
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = sourceCompatibility

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xstrict-java-nullability-assertions"
            )
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
                )
            )
        }
    }

    dependencyUpdates {
        checkForGradleUpdate = true
        outputFormatter = "html" // plain. json, xml
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}


fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA", "RC").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

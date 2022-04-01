import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")

    `maven-publish`

    id("com.github.ben-manes.versions")
}

version = "0.3.0-SNAPSHOT"
group = "xyz.chrisime"
description = "CRooD (an easy-to-use CRUD Repository built on jOOQ)"

dependencies {
    api(libs.jooq)
    compileOnly(libs.bundles.jooq)

    implementation(platform(libs.kotlin.bom))

    implementation(libs.json)

    testImplementation(platform(libs.junit))
    testImplementation(libs.bundles.kotest) {
        exclude(group = "io.mockk")
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = sourceCompatibility
            apiVersion = "1.6"
            languageVersion = "1.6"

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xstrict-java-nullability-assertions"
            )
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = sourceCompatibility
            apiVersion = "1.6"
            languageVersion = "1.6"

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xstrict-java-nullability-assertions"
            )
        }
    }

    jar {
        metaInf {
            from("${rootProject.path}/README.md", "${rootProject.path}/LICENSE")
        }

        manifest {
            attributes(
                mapOf(
                    "Bundle-Description" to rootProject.description,
                    "Bundle-Copyright" to copyright,
                    "Build-Jdk-Spec" to jdkSpecVersion,
                    "Build-Jdk" to jdkBuild,
                    "Build-OS" to buildOs,
                    "Bundle-Name" to rootProject.name,
                    "Bundle-SymbolicName" to artifactName,
                    "Bundle-Version" to project.version,
                    "Bundle-License" to pomLicenseUrl,
                    "Built-By" to builtBy,
                    "Created-By" to "Gradle ${gradle.gradleVersion}"
                )
            )
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    test {
        useJUnitPlatform()

        testLogging {
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
            displayGranularity = 2

            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
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
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

val username = "chrisime"
val myname = "Christian Meyer"
val myemail = "code@chrisime.xyz"
val copyright = "Copyright (c) 2016-2022"
val githubRepository = "${username}/crood"
val githubReadme = "README.md"

val buildOs = "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
val builtBy = "$myname <${myemail}>"

val jdkSpecVersion = System.getProperty("java.specification.version") as String
val jdkBuild = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"

val artifactName = project.name.toLowerCase()
val artifactGroup = project.group.toString()
val artifactVersion = project.version.toString()

val pomUrl = "https://github.com/${githubRepository}"
val pomScmUrl = "https://github.com/${githubRepository}"
val pomIssueUrl = "https://github.com/${githubRepository}/issues"
val pomDesc = "https://github.com/${githubRepository}"

val pomLicenseName = "Apache-2.0"
val pomLicenseUrl = "https://opensource.org/licenses/Apache-2.0"
val pomLicenseDist = "repo"

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>(artifactName) {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion

            from(components.getByName("java"))
            artifact(sourcesJar)

            pom {
                name.set(artifactName)
                description.set(pomDesc)
                url.set(pomUrl)

                licenses {
                    name.set(pomLicenseName)
                    url.set(pomLicenseUrl)
                    distributionManagement { }
                }

                developers {
                    developer {
                        id.set(username)
                        name.set(myname)
                        email.set(myemail)
                    }
                }

                contributors {  }

                scm {
                    url.set(pomScmUrl)
                }

                organization {  }

                issueManagement {  }

                ciManagement {  }

                distributionManagement {  }
            }
        }
    }
}

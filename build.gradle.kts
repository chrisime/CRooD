plugins {
    kotlin("jvm") version "1.4.21"
    `java-library`

    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"

    id("com.github.ben-manes.versions") version "0.36.0"
}

version = "0.2.0"
group = "xyz.chrisime"
description = "CRooD (an easy-to-use CRUD Base Repository built upon jOOQ)"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    api(platform("org.jooq:jooq-parent:3.14.4"))
    compileOnly("org.jooq:jooq-codegen")
    compileOnly("org.jooq:jooq-meta")

    api("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    compileOnly("org.slf4j:slf4j-api")

    testImplementation("org.jetbrains.kotlin:kotlin-test") {
        isTransitive = false
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit") {
        isTransitive = false
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
            apiVersion = "1.4"
            languageVersion = "1.4"

            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xstrict-java-nullability-assertions"
            )
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = sourceCompatibility
            apiVersion = "1.4"
            languageVersion = "1.4"

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
                    "Bundle-Name" to "crood",
                    "Bundle-Version" to project.version,
                    "Bundle-License" to pomLicenseUrl,
                    "Built-By" to "Gradle ${gradle.gradleVersion}",
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

val username = "chrisime"
val name = "Christian Meyer"
val githubRepository = "${username}/crood"
val githubReadme = "README.md"

val artifactName = "crood"
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

            from(components["java"])
            artifact(sourcesJar)

            pom.withXml {
                asNode().apply {
                    appendNode("name", artifactName) // rootProject.name
                    appendNode("description", pomDesc)
                    appendNode("url", pomUrl)
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", pomLicenseName)
                        appendNode("url", pomLicenseUrl)
                        appendNode("distribution", pomLicenseDist)
                    }
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("id", username)
                        appendNode("name", name)
                    }
                    appendNode("scm").apply {
                        appendNode("url", pomScmUrl)
                    }
                }
            }
        }
    }
}

bintray {
    user = project.findProperty("bintrayUser")?.toString() ?: System.getenv("bintrayUser") ?: ""
    key = project.findProperty("bintrayKey")?.toString() ?: System.getenv("bintrayApiKey") ?: ""
    publish = true

    setPublications(artifactName)

    pkg.apply {
        this.repo = "oss"
        this.name = artifactName
        this.userOrg = username
        this.githubRepo = githubRepository
        this.vcsUrl = pomScmUrl
        description = "CRUD Repository to be used in conjunction with included domain generator."
        this.setLabels("kotlin", "java", "jooq", "crud", "code generation")
        this.setLicenses(pomLicenseName)
        this.desc = description
        this.websiteUrl = pomUrl
        this.issueTrackerUrl = pomIssueUrl
        this.githubReleaseNotesFile = githubReadme

        version.apply {
            name = artifactVersion
            desc = pomDesc
//            released = ""
            vcsTag = artifactVersion
        }
    }
}

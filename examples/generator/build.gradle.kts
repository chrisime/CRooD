plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

group = "xyz.chrisime"
version = "1.0-SNAPSHOT"
description = "jOOQ Generator using Test Containers"

dependencies {
    implementation("org.postgresql", "postgresql", "42.3.4")
    implementation("org.flywaydb", "flyway-core", "8.+")

    implementation("org.jooq", "jooq", "3.16.+")
    implementation("org.jooq", "jooq-meta", "3.16.+")

    implementation("org.testcontainers", "postgresql", "1.17.+") {
        exclude(group = "org.slf4j")
    }
    runtimeOnly("ch.qos.logback", "logback-classic", "1.2.11")
    runtimeOnly("org.slf4j", "slf4j-api", "1.7.36")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

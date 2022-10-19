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
    implementation("org.postgresql", "postgresql", "42.5.0")
    implementation("org.flywaydb", "flyway-core", "8.5.13")

    implementation("org.jooq", "jooq", "3.17.4")
    implementation("org.jooq", "jooq-meta", "3.17.4")

    implementation("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.0")

    implementation("org.testcontainers", "postgresql", "1.17.5")

    runtimeOnly("ch.qos.logback", "logback-classic", "1.2.11")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

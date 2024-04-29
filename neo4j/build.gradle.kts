plugins {
    kotlin("jvm") version "1.9.22"
}

group = "org.parser.neo4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":core"))

    implementation("org.neo4j:neo4j-kernel:5.19.0")
    testImplementation("org.neo4j.test:neo4j-harness:5.19.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
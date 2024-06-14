plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.parser"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    testImplementation("org.junit.jupiter:junit-jupiter-params:5.1.0")
    testImplementation("org.neo4j:neo4j:4.4.33")
    testImplementation("org.apache.jena:jena-core:4.10.0")

    testImplementation(project(":core"))
    testImplementation(project(":neo4j"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
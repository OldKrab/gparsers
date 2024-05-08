plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")

}

group = "org.parser"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":core"))

    implementation("org.neo4j:neo4j-kernel:4.4.33")
    testImplementation("org.neo4j.test:neo4j-harness:4.4.33")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "graphParserCombinators-neo4j"
            from(components["java"])
        }
    }
}
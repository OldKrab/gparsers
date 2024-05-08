plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")
}

group = "org.parser"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}


task("dependencyList") {
    doFirst {
        println(configurations.runtimeClasspath.get().files.joinToString(separator = ":"))
    }
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "graphParserCombinators"
            from(components["java"])
        }
    }
}

plugins {
    kotlin("jvm") version "1.9.22"
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
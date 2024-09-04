import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "com.toms223"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation(platform("org.http4k:http4k-bom:5.13.8.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-jetty")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile>{
    kotlinOptions {
        javaParameters = true
    }
}
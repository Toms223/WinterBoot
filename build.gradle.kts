plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jetbrains.dokka") version "2.0.0"
    id("maven-publish")

}

group = "io.github.toms223"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation(platform("org.http4k:http4k-bom:5.45.2.0"))
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.toms223"
            artifactId = "winterboot"
            version = "2.0.1"
            from(components["java"])
        }
    }
}
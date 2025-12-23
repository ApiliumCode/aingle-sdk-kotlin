plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    `maven-publish`
    signing
}

group = "com.apilium"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // HTTP Client
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
    implementation("io.ktor:ktor-client-websockets:2.3.8")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.ktor:ktor-client-mock:2.3.8")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("AIngle SDK for Kotlin")
                description.set("Official Kotlin SDK for AIngle - the ultra-light distributed ledger for IoT devices")
                url.set("https://github.com/ApiliumCode/aingle-sdk-kotlin")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("apilium")
                        name.set("Apilium Technologies")
                        email.set("hello@apilium.com")
                    }
                }

                scm {
                    url.set("https://github.com/ApiliumCode/aingle-sdk-kotlin")
                    connection.set("scm:git:git://github.com/ApiliumCode/aingle-sdk-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/ApiliumCode/aingle-sdk-kotlin.git")
                }
            }
        }
    }
}

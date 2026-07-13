plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    `maven-publish`
    signing
}

group = "com.apilium"
version = "0.2.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))

    // Coroutines (core + JDK8/CompletableFuture integration for HttpClient.sendAsync)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // HTTP transport is the JDK 11+ java.net.http.HttpClient (no extra dependency).

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
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
                description.set("Official Kotlin SDK for AIngle, the verifiable memory cortex for AI agents.")
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

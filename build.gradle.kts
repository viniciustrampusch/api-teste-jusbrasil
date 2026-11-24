import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    id("org.springframework.boot") version "4.2.0"
    jacoco
    id("com.diffplug.spotless") version "8.12.0"
    id("dev.detekt") version "2.0.0-alpha.6"
}

// Version comes from the Spring Boot plugin so it stays aligned with the BOM.
apply(plugin = "io.spring.dependency-management")

group = "interview-jusbrasil"
version = "0.1.0"
description = "Minimal Kotlin and Spring Boot baseline for the engineering environment."

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.14.12")
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
                implementation("org.wiremock:wiremock-standalone:3.13.2")
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(tasks.named("test"))
                    }
                }
            }
        }
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    doFirst {
        val dotEnv = project.file(".env")
        if (!dotEnv.isFile) {
            return@doFirst
        }
        dotEnv.readLines().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                return@forEach
            }
            val separator = line.indexOf("=")
            if (separator <= 0) {
                return@forEach
            }
            val key = line.substring(0, separator).trim()
            val rawValue = line.substring(separator + 1).trim()
            val value =
                if (rawValue.length >= 2 &&
                    (
                        (rawValue.startsWith("\"") && rawValue.endsWith("\"")) ||
                            (rawValue.startsWith("'") && rawValue.endsWith("'"))
                    )
                ) {
                    rawValue.substring(1, rawValue.length - 1)
                } else {
                    rawValue
                }
            if (key.isNotEmpty() && System.getenv(key) == null) {
                environment(key, value)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

jacoco {
    toolVersion = "0.8.16"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(
        "spotlessCheck",
        "integrationTest",
        "detektMain",
        "detektTest",
        "detektIntegrationTest",
    )
}

tasks.register("fastCheck") {
    group = "verification"
    description = "Formatting, static analysis, and unit tests."
    dependsOn("spotlessCheck", "detektMain", "detektTest", "test")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("config/detekt/detekt.yml"))
}

// Spring Boot aligns every org.jetbrains.kotlin dependency to 2.4.21.
// Detekt 2.0.0-alpha.6 cannot run with a compiler newer than the one it was built with.
configurations.configureEach {
    if (name.contains("detekt")) {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.4.10")
                because("Detekt 2.0.0-alpha.6 was compiled with Kotlin 2.4.10")
            }
        }
    }
}

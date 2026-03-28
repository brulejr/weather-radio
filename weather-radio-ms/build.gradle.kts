import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ksbCommonsVersion: String by project
val projectVersion: String by project

val useLocalKsbCommonsString: String = "true"
val useLocalKsbCommons: Boolean = useLocalKsbCommonsString.toBoolean()

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.jrb.labs"
description = "Offline NOAA weather radio phase 1 stub service"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

/**
 * Resolve the project version:
 *
 * - In CI on a tag build, GITHUB_REF_NAME will be something like "v0.3.1".
 *   We strip the leading "v" and use "0.3.1" as the version.
 * - Otherwise, fall back to projectVersion from gradle.properties.
 */
version = System.getenv("GITHUB_REF_NAME")
    ?.let { refName ->
        if (refName.matches(Regex("""v\d+\.\d+\.\d+"""))) {
            refName.removePrefix("v")
        } else {
            projectVersion
        }
    }
    ?: projectVersion

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()

    if (!useLocalKsbCommons) {
        maven {
            url = uri("https://maven.pkg.github.com/brulejr/ksb-commons")
            credentials {
                username = findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_PACKAGES_USER")
                            ?: System.getenv("GITHUB_ACTOR")
                            ?: "brulejr"
                password = findProperty("gpr.key") as String?
                    ?: System.getenv("GITHUB_PACKAGES_TOKEN")
                            ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    if (useLocalKsbCommons) {
        implementation(platform("io.jrb.labs:ksb-dependency-bom"))
    } else {
        implementation(platform("io.jrb.labs:ksb-dependency-bom:$ksbCommonsVersion"))
    }
    implementation("io.jrb.labs:ksb-commons-ms-core")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}


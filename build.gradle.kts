import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.spring") version "1.3.71"
}

group = "no.esa"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val springBoot = "org.springframework.boot"
    val jetBrains = "org.jetbrains.kotlin"

    developmentOnly("$springBoot:spring-boot-devtools")
    implementation("$springBoot:spring-boot-starter-jdbc")
    implementation("$springBoot:spring-boot-starter-web")
    testImplementation("$springBoot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin")
    implementation(group = jetBrains, name = "kotlin-reflect")
    implementation(group = jetBrains, name = "kotlin-stdlib-jdk8")
    implementation(group = "org.flywaydb", name = "flyway-core", version = "6.4.1")
    implementation(group = "org.postgresql", name = "postgresql", version = "42.2.12")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

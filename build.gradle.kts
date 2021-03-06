import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.spring") version "1.3.71"
    application
}

apply(plugin = "org.openapi.generator")

application {
    applicationName = "Battleship"
    mainClassName = "no.esa.battleship.application.BattleshipApplication.kt"
}

group = "no.esa"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated-sources/src/main")
        }
    }
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        jcenter()
    }

    dependencies {
        classpath(group = "org.jetbrains.kotlin", name = "kotlin-allopen", version = "1.3.71")
        classpath(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = "1.3.71")
        classpath(group = "org.springframework.boot", name = "spring-boot-gradle-plugin", version = "2.2.0.M3")
        classpath(group = "io.spring.gradle", name = "dependency-management-plugin", version = "1.0.6.RELEASE")
        classpath(group = "org.openapitools", name = "openapi-generator-gradle-plugin", version = "4.2.3")
    }
}

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
    implementation("$springBoot:spring-boot-starter-aop")
    testImplementation("$springBoot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(group = jetBrains, name = "kotlin-test-annotations-common", version = "1.3.71")
    testImplementation(group = jetBrains, name = "kotlin-test", version = "1.3.71")
    testImplementation(group = jetBrains, name = "kotlin-test-common", version = "1.3.71")
    implementation(group = jetBrains, name = "kotlin-reflect")
    implementation(group = jetBrains, name = "kotlin-stdlib-jdk8")
    testImplementation(group = "io.mockk", name = "mockk", version = "1.10.0")
    testImplementation(group = "com.willowtreeapps.assertk", name = "assertk", version = "0.22")
    testImplementation(group = "com.h2database", name = "h2", version = "1.4.200")

    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin")
    //implementation(group = jetBrains + "x", name = "kotlinx-coroutines-core", version = "1.3.7")
    implementation(group = "org.flywaydb", name = "flyway-core", version = "6.4.1")
    implementation(group = "org.postgresql", name = "postgresql", version = "42.2.14")
    implementation(group = "io.springfox", name = "springfox-swagger2", version = "2.9.2")
    implementation(group = "io.springfox", name = "springfox-swagger-ui", version = "2.9.2")
    //implementation(group = "org.springframework.retry", name = "spring-retry", version = "1.2.5.RELEASE")
    implementation(kotlin("stdlib-jdk8"))
}
tasks {
    create<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("buildKotlinClient") {
        val generationPath = "$buildDir/generated-sources"
        val basePackage = "battleship"

        delete(generationPath)
        generatorName.set("kotlin-spring")
        inputSpec.set("$rootDir/src/main/resources/api-definition/index.yaml")
        outputDir.set(generationPath)
        apiPackage.set("$basePackage.api")
        modelPackage.set("$basePackage.model")
        configOptions.set(mapOf(
                "dateLibrary" to "java8",
                "swaggerAnnotations" to "true",
                "interfaceOnly" to "true"))
        systemProperties.set(mapOf(
                "apis" to "",
                "models" to "",
                "generateSupportingFiles" to "false"))
    }

    withType<KotlinCompile> {
        dependsOn("buildKotlinClient")
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

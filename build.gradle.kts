import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.23"

    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
}

group = "no.nav.amt-altinn-acl"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

val commonVersion = "3.2023.12.12_13.53-510909d4aa1a"
val testcontainersVersion = "1.19.7"
val logstashEncoderVersion = "7.4"
val shedlockVersion = "5.12.0"
val tokenSupportVersion = "4.1.3"
val okHttpVersion = "4.12.0"
val mockkVersion = "1.13.10"
val kotestVersion = "5.8.1"
val mockOauth2ServerVersion = "2.1.2"

extra["postgresql.version"] = "42.7.2"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-core")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")

    implementation("no.nav.common:rest:$commonVersion")
    implementation("no.nav.common:token-client:$commonVersion")
    implementation("no.nav.common:job:$commonVersion")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("com.vaadin.external.google", "android-json")
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

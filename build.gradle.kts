plugins {
    val kotlinVersion = "2.2.21"

    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
}

group = "no.nav.amt-altinn-acl"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

val commonVersion = "3.2025.11.10_14.07-a9f44944d7bc"
val logstashEncoderVersion = "9.0"
val shedlockVersion = "7.5.0"
val tokenSupportVersion = "6.0.1"
val okHttpVersion = "5.3.2"
val mockkVersion = "1.14.7"
val kotestVersion = "6.0.7"
val mockOauth2ServerVersion = "3.0.1"
val unleashVersion = "11.2.1"
val springmockkVersion = "5.0.1"
val jacksonModuleKotlinVersion = "3.0.3"

dependencyManagement {
    dependencies {
        dependency("com.squareup.okhttp3:okhttp:$okHttpVersion")
        dependency("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-flyway")

    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")

    implementation("no.nav.common:rest:$commonVersion")
    implementation("no.nav.common:token-client:$commonVersion")
    implementation("no.nav.common:job:$commonVersion")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.getunleash:unleash-client-java:$unleashVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk-jvm:$mockkVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
}

kotlin {
    jvmToolchain(24)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xmulti-dollar-interpolation",
        )
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
    )
}

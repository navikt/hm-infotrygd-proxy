import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.23"
}

val konfig_version: String by project
val ojdbc_version: String by project
val unleash_version: String by project
val prometheus_version: String by project

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

fun ktor(name: String) = "io.ktor:ktor-$name:2.3.10"

dependencies {
    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("ch.qos.logback:logback-classic:1.5.5")

    // Ktor Server
    implementation(ktor("server-netty"))
    implementation(ktor("server-core"))
    implementation(ktor("server-auth"))
    implementation(ktor("server-auth-jwt"))
    implementation(ktor("serialization-jackson"))
    implementation(ktor("server-metrics-micrometer"))
    implementation(ktor("server-content-negotiation"))
    implementation(ktor("server-call-id"))

    // Ktor Client
    implementation(ktor("client-core"))
    implementation(ktor("client-auth"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-jackson"))
    implementation(ktor("client-content-negotiation"))

    implementation("com.natpryce:konfig:$konfig_version")
    implementation("com.oracle.database.jdbc:ojdbc8:$ojdbc_version")
    implementation("no.finn.unleash:unleash-client-java:$unleash_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheus_version")
    val jacksonVersion = "2.15.3"
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Test
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("no.nav.hjelpemidler.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

tasks.test {
    useJUnitPlatform()
}

val fatJar = task("fatJar", type = org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.23"
}

val konfig_version: String by project
val ojdbc_version: String by project
val ktor_version: String by project
val unleash_version: String by project
val prometheus_version: String by project
val jackson_version: String by project

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

fun ktor(name: String) = "io.ktor:ktor-$name:$ktor_version"

dependencies {
    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("ch.qos.logback:logback-classic:1.5.5")

    // Ktor Server
    implementation(ktor("server-netty"))
    implementation(ktor("server-core"))
    implementation(ktor("auth"))
    implementation(ktor("auth-jwt"))
    implementation(ktor("jackson"))
    implementation(ktor("metrics-micrometer"))

    // Ktor Client
    implementation(ktor("client-core"))
    implementation(ktor("client-auth"))
    implementation(ktor("client-apache"))
    implementation(ktor("client-jackson"))

    implementation("com.natpryce:konfig:$konfig_version")
    implementation("com.oracle.database.jdbc:ojdbc8:$ojdbc_version")
    implementation("no.finn.unleash:unleash-client-java:$unleash_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheus_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")

    // Test
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("no.nav.hjelpemidler.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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

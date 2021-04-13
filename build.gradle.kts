import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

val logging_version: String by project
val logback_version: String by project
val konfig_version: String by project
val klaxon_version: String by project
val ojdbc_version: String by project
val ktor_version: String by project
val unleash_version: String by project

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // Used for Rapids and rivers-dependency
    maven("https://packages.confluent.io/maven/") // Kafka-avro
    jcenter()
}

dependencies {
    testImplementation(kotlin("test-junit"))

    implementation("io.github.microutils:kotlin-logging:$logging_version")
    implementation("com.natpryce:konfig:$konfig_version")
    implementation("com.beust:klaxon:$klaxon_version")
    implementation("com.oracle.database.jdbc:ojdbc8:$ojdbc_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-client-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("junit:junit:4.12")
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")
    implementation("no.finn.unleash:unleash-client-java:$unleash_version")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "15"
}

val fatJar = task("fatJar", type = org.gradle.jvm.tasks.Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Main-Class"] = "no.nav.hjelpemidler.ApplicationKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}

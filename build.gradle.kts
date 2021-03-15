import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}
val rapid_version: String by project
val logging_version: String by project
val konfig_version: String by project
val kafka_version: String by project
val influxdb_version: String by project
val klaxon_version: String by project
val ojdbc_version: String by project

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // Used for Rapids and rivers-dependency
    maven("https://packages.confluent.io/maven/") // Kafka-avro
    jcenter()
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapid_version")
    implementation("io.github.microutils:kotlin-logging:$logging_version")
    implementation("com.natpryce:konfig:$konfig_version")
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("org.influxdb:influxdb-java:$influxdb_version")
    implementation("com.beust:klaxon:$klaxon_version")
    implementation("com.oracle.database.jdbc:ojdbc8:$ojdbc_version")
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

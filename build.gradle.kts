plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.hm.core)

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.bundles.logging.runtime)

    // Ktor Server
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)

    // Database
    implementation(libs.kotliquery)
    implementation(libs.ucp11)
    runtimeOnly(libs.ojdbc11)

    // Jackson
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Test
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2)
}

spotless {
    kotlin {
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_comment-wrapping" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
                "ktlint_standard_value-argument-comment" to "disabled",
                "ktlint_standard_value-parameter-comment" to "disabled",
            ),
        )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

application { mainClass.set("no.nav.hjelpemidler.infotrygd.proxy.ApplicationKt") }
kotlin { jvmToolchain(21) }
tasks.test { useJUnitPlatform() }

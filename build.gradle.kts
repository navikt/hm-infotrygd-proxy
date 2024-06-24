plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.hm.core)

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.bundles.logging.runtime)

    // Jackson
    implementation(libs.bundles.jackson)

    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // Database
    implementation(libs.hm.database)
    implementation(libs.hm.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:hm-database-oracle")
        }
    }

    // Test
    testImplementation(libs.bundles.test)
    testImplementation(libs.hm.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:hm-database-h2")
        }
    }
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

tasks.test {
    environment("NAIS_CLUSTER_NAME", "test")
    useJUnitPlatform()
}

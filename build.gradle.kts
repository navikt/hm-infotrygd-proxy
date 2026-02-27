plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.micrometer.registry.prometheus)

    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // hotlibs
    implementation(platform(libs.hotlibs.platform))
    implementation(libs.hotlibs.core)
    implementation(libs.hotlibs.logging)
    implementation(libs.hotlibs.serialization)

    // hotlibs/database
    implementation(libs.hotlibs.database)
    implementation(libs.hotlibs.database) {
        capabilities {
            requireCapability("no.nav.hjelpemidler:database-oracle")
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

application {
    mainClass.set("no.nav.hjelpemidler.infotrygd.proxy.ApplicationKt")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

testing {
    @Suppress("UnstableApiUsage")
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest(libs.versions.kotlin.asProvider())

            dependencies {
                implementation(libs.hotlibs.test)
                implementation(libs.hotlibs.database) {
                    capabilities {
                        requireCapability("no.nav.hjelpemidler:database-h2")
                    }
                }
            }

            targets.all {
                testTask {
                    environment("NAIS_CLUSTER_NAME", "test")
                }
            }
        }
    }
}

tasks.shadowJar {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

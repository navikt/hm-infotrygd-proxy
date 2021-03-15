package no.nav.hjelpemidler.configuration

import com.natpryce.konfig.*

internal object Configuration {

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
        "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
        else -> {
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
        }
    }

    private val prodProperties = ConfigurationMap(
        mapOf(
        )
    )

    private val devProperties = ConfigurationMap(
        mapOf(
        )
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "HM_INFOTRYGD_PROXY_DB_URL" to "abc",
            "HM_INFOTRYGD_PROXY_DB_USR" to "abc",
            "HM_INFOTRYGD_PROXY_DB_PW" to "abc",
        )
    )

    val oracleDatabaseConfig: Map<String, String> = mapOf(
        "HM_INFOTRYGD_PROXY_DB_URL" to config()[Key("HM_INFOTRYGD_PROXY_DB_URL", stringType)],
        "HM_INFOTRYGD_PROXY_DB_USR" to config()[Key("HM_INFOTRYGD_PROXY_DB_USR", stringType)],
        "HM_INFOTRYGD_PROXY_DB_PW" to config()[Key("HM_INFOTRYGD_PROXY_DB_PW", stringType)],
    )

}
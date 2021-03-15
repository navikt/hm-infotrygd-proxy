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
            "HM_INFOTRYGDPROXY_SRVUSER" to "abc",
            "HM_INFOTRYGDPROXY_SRVPWD" to "abc",
        )
    )

    val oracleDatabaseConfig: Map<String, String> = mapOf(
        "HM_INFOTRYGDPROXY_SRVUSER" to config()[Key("HM_INFOTRYGDPROXY_SRVUSER", stringType)],
        "HM_INFOTRYGDPROXY_SRVPWD" to config()[Key("HM_INFOTRYGDPROXY_SRVPWD", stringType)],
        "DATABASE_URL" to "jdbc:oracle:thin:@a01dbfl033.adeo.no:1521/infotrygd_hjq",
    )

}
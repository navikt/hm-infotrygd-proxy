package no.nav.hjelpemidler.configuration

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val log = KotlinLogging.logger {}

object Configuration {
    private fun vaultSecret(filename: String): Lazy<String> = lazy {
        runCatching { File(filename).readText(Charsets.UTF_8) }.getOrElse {
            log.warn(it) { "Kunne ikke lese filename: $filename" }
            ""
        }
    }

    private val configuration by lazy {
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
            "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
            else ->
                ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
        }
    }

    val HM_INFOTRYGD_PROXY_DB_JDBC_URL by vaultSecret("/secrets/infotrygd_hj/config/jdbc_url")
    val HM_INFOTRYGD_PROXY_DB_USERNAME by vaultSecret("/secrets/infotrygd_hj/credentials/username")
    val HM_INFOTRYGD_PROXY_DB_PASSWORD by vaultSecret("/secrets/infotrygd_hj/credentials/password")
    val HM_INFOTRYGD_PROXY_DB_NAME by lazy { configuration[Key("HM_INFOTRYGD_PROXY_DB_NAME", stringType)] }

    private val prodProperties = ConfigurationMap(
        "APPLICATION_PROFILE" to "prod",

        "HM_INFOTRYGD_PROXY_DB_NAME" to "INFOTRYGD_HJP",
    )

    private val devProperties = ConfigurationMap(
        "APPLICATION_PROFILE" to "dev",

        "HM_INFOTRYGD_PROXY_DB_NAME" to "INFOTRYGD_HJQ",
    )

    private val localProperties = ConfigurationMap(
        "APPLICATION_PROFILE" to "local",

        "HM_INFOTRYGD_PROXY_DB_NAME" to "INFOTRYGD_HJL",

        "AZURE_APP_WELL_KNOWN_URL" to "http://azure/.well-known/openid-configuration",
        "AZURE_APP_CLIENT_ID" to "hm-infotrygd-proxy",
    )

    val application: Map<String, String> = mapOf(
        "APPLICATION_PROFILE" to configuration[Key("APPLICATION_PROFILE", stringType)],
    )

    val azureAD: Map<String, String> = mapOf(
        "AZURE_APP_WELL_KNOWN_URL" to configuration[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
        "AZURE_APP_CLIENT_ID" to configuration[Key("AZURE_APP_CLIENT_ID", stringType)],
    )
}

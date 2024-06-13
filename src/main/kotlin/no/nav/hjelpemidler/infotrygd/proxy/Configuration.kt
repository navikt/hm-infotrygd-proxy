package no.nav.hjelpemidler.infotrygd.proxy

import no.nav.hjelpemidler.configuration.EnvironmentVariable
import no.nav.hjelpemidler.configuration.External
import no.nav.hjelpemidler.configuration.vaultSecret

object Configuration {
    // Database
    val HM_INFOTRYGD_PROXY_DB_JDBC_URL by vaultSecret("/secrets/infotrygd_hj/config/jdbc_url")
    val HM_INFOTRYGD_PROXY_DB_USERNAME by vaultSecret("/secrets/infotrygd_hj/credentials/username")
    val HM_INFOTRYGD_PROXY_DB_PASSWORD by vaultSecret("/secrets/infotrygd_hj/credentials/password")
    val HM_INFOTRYGD_PROXY_DB_NAME by EnvironmentVariable

    // Entra ID
    @External
    val AZURE_APP_CLIENT_ID by EnvironmentVariable

    @External
    val AZURE_OPENID_CONFIG_ISSUER by EnvironmentVariable

    @External
    val AZURE_OPENID_CONFIG_JWKS_URI by EnvironmentVariable
}

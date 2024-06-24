package no.nav.hjelpemidler.infotrygd.proxy

import no.nav.hjelpemidler.configuration.EnvironmentVariable
import no.nav.hjelpemidler.configuration.vaultSecret

object Configuration {
    // Database
    val HM_INFOTRYGD_PROXY_DB_JDBC_URL by vaultSecret("/secrets/infotrygd_hj/config/jdbc_url")
    val HM_INFOTRYGD_PROXY_DB_USERNAME by vaultSecret("/secrets/infotrygd_hj/credentials/username")
    val HM_INFOTRYGD_PROXY_DB_PASSWORD by vaultSecret("/secrets/infotrygd_hj/credentials/password")
    val HM_INFOTRYGD_PROXY_DB_NAME by EnvironmentVariable
}

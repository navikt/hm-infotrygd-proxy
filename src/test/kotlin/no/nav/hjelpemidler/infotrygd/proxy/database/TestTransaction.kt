package no.nav.hjelpemidler.infotrygd.proxy.database

import no.nav.hjelpemidler.database.H2
import no.nav.hjelpemidler.database.H2Mode
import no.nav.hjelpemidler.database.createDataSource
import no.nav.hjelpemidler.infotrygd.proxy.Database

suspend fun <T> testTransaction(block: Database.DaoProvider.() -> T): T {
    val database = createDataSource(H2) {
        mode = H2Mode.ORACLE
        initScript = "/infotrygd_hjl.sql"
    }.let(::Database)
    return database.use { it.transaction(block) }
}

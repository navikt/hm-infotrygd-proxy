package no.nav.hjelpemidler.infotrygd.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.database.transactionAsync
import no.nav.hjelpemidler.database.withDatabaseContext
import java.io.Closeable
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

class Database(private val dataSource: DataSource) : Closeable {
    private val schema = Configuration.HM_INFOTRYGD_PROXY_DB_NAME

    suspend fun isValid(): Boolean = withDatabaseContext {
        dataSource.connection.use {
            it.isValid(10)
        }
    }

    suspend fun <T> transaction(block: DaoProvider.() -> T): T = transactionAsync(dataSource, strict = true) {
        it.execute("""ALTER SESSION SET CURRENT_SCHEMA = $schema""")
        DaoProvider(it).block()
    }

    override fun close() {
        if (dataSource is Closeable) {
            log.info { "Stopper databasen..." }
            dataSource.close()
        }
    }

    class DaoProvider(jdbcOperations: JdbcOperations) {
        val infotrygdDao = InfotrygdDao(jdbcOperations)
    }
}

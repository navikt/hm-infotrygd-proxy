package no.nav.hjelpemidler.infotrygd.proxy.database

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.sessionOf
import no.nav.hjelpemidler.infotrygd.proxy.InfotrygdDao
import java.io.Closeable
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

class Database(private val dataSource: DataSource) : Closeable {
    suspend fun isValid(): Boolean = withDatabaseContext {
        dataSource.connection.use {
            it.isValid(10)
        }
    }

    suspend fun <T> transaction(block: DaoProvider.() -> T): T = withDatabaseContext {
        sessionOf(dataSource, strict = true).use { session ->
            session.transaction { tx ->
                DaoProvider(SessionJdbcOperations(tx)).block()
            }
        }
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

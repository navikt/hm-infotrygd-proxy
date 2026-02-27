package no.nav.hjelpemidler.infotrygd.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.TestEnvironment
import no.nav.hjelpemidler.database.JdbcOperations
import java.io.Closeable
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

class Database(private val dataSource: DataSource) : Closeable {
    suspend fun isValid(): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use {
            it.isValid(10)
        }
    }

    suspend fun <T> transaction(block: DaoProvider.() -> T): T = no.nav.hjelpemidler.database.transaction(dataSource, strict = true) {
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

/**
 * NB! Kan ikke opprette temporary table i CURRENT_SCHEMA, kun i schema for bruker.
 */
@JvmInline
value class TemporaryTableName(private val value: String) {
    private val schema get() = Configuration.HM_INFOTRYGD_PROXY_DB_USERNAME

    override fun toString(): String = if (Environment.current != TestEnvironment) {
        "$schema.ORA\$PTT_$value"
    } else {
        "ORA\$PTT_$value"
    }
}

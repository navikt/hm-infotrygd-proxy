package no.nav.hjelpemidler.infotrygd.proxy.database

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.infotrygd.proxy.InfotrygdDao
import java.io.Closeable
import java.sql.Connection
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

class Database(
    private val dataSource: DataSource,
    private val schema: String,
) : Closeable {
    suspend fun isValid(): Boolean = withDatabaseContext {
        dataSource.connection.use {
            it.isValid(10)
        }
    }

    suspend fun <T> invoke(block: DaoProvider.() -> T): T = withDatabaseContext {
        dataSource.connection.use { connection ->
            val jdbcOperations: JdbcOperations = ConnectionJdbcOperations(connection)
            jdbcOperations.execute("ALTER SESSION SET CURRENT_SCHEMA = $schema")
            block(DaoProvider(jdbcOperations))
        }
    }

    suspend fun <T> withConnection(block: (Connection) -> T): T = withDatabaseContext {
        dataSource.connection.use {
            it.createStatement().use { statement ->
                statement.execute("ALTER SESSION SET CURRENT_SCHEMA = $schema")
            }
            block(it)
        }
    }

    override fun close() {
        log.info { "Stopper databasen" }
        if (dataSource is Closeable) {
            dataSource.close()
        }
    }

    class DaoProvider(jdbcOperations: JdbcOperations) {
        val infotrygdDao = InfotrygdDao(jdbcOperations)
    }
}

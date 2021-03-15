package no.nav.hjelpemidler

import mu.KotlinLogging
import no.nav.hjelpemidler.configuration.Configuration
import oracle.jdbc.OracleConnection
import oracle.jdbc.pool.OracleDataSource
import java.util.Properties

private val logg = KotlinLogging.logger {}
// private val sikkerlogg = KotlinLogging.logger("tjenestekall")

fun main() {
    logg.info("Hello world")

    // Set up database connection
    val info = Properties()
    info[OracleConnection.CONNECTION_PROPERTY_USER_NAME] = Configuration.oracleDatabaseConfig["HM_INFOTRYGDPROXY_SRVUSER"]!!
    info[OracleConnection.CONNECTION_PROPERTY_PASSWORD] = Configuration.oracleDatabaseConfig["HM_INFOTRYGDPROXY_SRVPWD"]!!
    info[OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH] = "20"

    val ods = OracleDataSource()
    ods.url = Configuration.oracleDatabaseConfig["DATABASE_URL"]!!
    ods.connectionProperties = info

    try {
        logg.info("Connecting to database")
        val connection = ods.getConnection()

        logg.info("Fetching db metadata")
        val dbmd = connection.metaData
        println("Driver Name: " + dbmd.driverName)
        println("Driver Version: " + dbmd.driverVersion)

        println("")
        println("Tables we have access to:")
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT owner, table_name FROM all_tables").use { resultSet ->
                while (resultSet.next()) {
                    println(resultSet.getString(1) + " " + resultSet.getString(2) + " ")
                }
            }
        }

        println("")
        println("All tables:")
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT table_name FROM dba_tables").use { resultSet ->
                while (resultSet.next()) {
                    println(resultSet.getString(1) + " ")
                }
            }
        }

    } catch (e: Exception) {
        println("Exception: " + e.message.toString())
        e.printStackTrace()
    }

    logg.info("Processing done, sleeping forever.")
    Thread.sleep(1000*60*60*24)
}

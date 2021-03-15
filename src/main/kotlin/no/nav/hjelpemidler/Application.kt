package no.nav.hjelpemidler

import mu.KotlinLogging
import no.nav.hjelpemidler.configuration.Configuration
import oracle.jdbc.OracleConnection
import oracle.jdbc.pool.OracleDataSource
import java.sql.PreparedStatement
import java.util.Properties

private val logg = KotlinLogging.logger {}
// private val sikkerlogg = KotlinLogging.logger("tjenestekall")

private var prStmtDecisionResult: PreparedStatement? = null

fun main() {
    logg.info("Hello world")

    // Set up database connection
    val info = Properties()
    info[OracleConnection.CONNECTION_PROPERTY_USER_NAME] = Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_USR"]!!
    info[OracleConnection.CONNECTION_PROPERTY_PASSWORD] = Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_PW"]!!
    info[OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH] = "20"

    val ods = OracleDataSource()
    ods.url = Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_URL"]!!
    ods.connectionProperties = info

    logg.info("Connecting to database")
    val connection = ods.getConnection()

    logg.info("Fetching db metadata")
    val dbmd = connection.metaData
    logg.info("Driver Name: " + dbmd.driverName)
    logg.info("Driver Version: " + dbmd.driverVersion)

    prStmtDecisionResult = connection.prepareStatement("SELECT S10_RESULTAT as resultat FROM INFOTRYGD_HJQ.SA_SAK_10 WHERE S01_PERSONKEY = ? AND S05_SAKSBLOKK = ? AND S10_SAKSNR = ?")

    val testFakeTkNr = "2103"
    val testFakeFNR = "07010589518"
    val testFakeSaksBlokk = "A"
    val testFakeSaksNr = "04"

    val result = queryForDecisionResult(testFakeTkNr, testFakeFNR, testFakeSaksBlokk, testFakeSaksNr)
    logg.info("Result was: \"$result\" (should be \"IM\")")

    prStmtDecisionResult!!.close()

    logg.info("Processing done, sleeping forever.")
    Thread.sleep(1000*60*60*24)
}

fun queryForDecisionResult(tkNr: String, fnr: String, saksblokk: String, saksnr: String): String {
    val stmt = prStmtDecisionResult!!

    stmt.clearParameters()
    stmt.setString(1, "$tkNr$fnr") // S01_PERSONKEY
    stmt.setString(2, saksblokk) // S05_SAKSBLOKK
    stmt.setString(3, saksnr) // S10_SAKSNR

    stmt.executeQuery().use { rs ->
        while (rs.next()) {
            return rs.getString("resultat")
        }
    }

    throw Exception("no such db row")
}
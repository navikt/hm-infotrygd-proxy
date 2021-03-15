package no.nav.hjelpemidler

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import io.ktor.auth.jwt.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.server.netty.*
import io.ktor.util.*
import java.util.*
import mu.KotlinLogging
import no.nav.hjelpemidler.configuration.Configuration
import oracle.jdbc.OracleConnection
import oracle.jdbc.pool.OracleDataSource
import org.json.simple.JSONObject
import org.slf4j.event.Level
import java.sql.Connection
import java.sql.PreparedStatement

private val logg = KotlinLogging.logger {}
// private val sikkerlogg = KotlinLogging.logger("tjenestekall")

private var dbConnection: Connection? = null

fun main(args: Array<String>) {
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
    dbConnection = ods.getConnection()

    logg.info("Fetching db metadata")
    val dbmd = dbConnection!!.metaData
    logg.info("Driver Name: " + dbmd.driverName)
    logg.info("Driver Version: " + dbmd.driverVersion)

    // Test query
    logg.info("Test query:")
    val testFakeTkNr = "2103"
    val testFakeFNR = "07010589518"
    val testFakeSaksBlokk = "A"
    val testFakeSaksNr = "04"
    val result = queryForDecisionResult(testFakeTkNr, testFakeFNR, testFakeSaksBlokk, testFakeSaksNr)
    logg.info("Result was: \"$result\" (should be \"IM\")")

    // Serve http REST API requests
    EngineMain.main(args)

    // Cleanup
    logg.info("Cleaning up and stopping.")
    dbConnection!!.close()
}

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    installAuthentication()

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }

    install(CallLogging) {
        level = Level.TRACE
        filter { call -> !call.request.path().startsWith("/internal") }
    }

    routing {
        authenticate("aad") {
            get("/result") {
                val req = call.receive<JSONObject>()
                try {
                    val tknr = req.get("tknr").toString()
                    val fnr = req.get("fnr").toString()
                    val saksblokk = req.get("saksblokk").toString()
                    val saksnr = req.get("saksnr").toString()

                    logg.info("Incoming authenticated request with tknr=$tknr fnr=$fnr saksblokk=$saksblokk saksnr=$saksnr")

                    // Proccess request
                    val res = queryForDecisionResult(tknr, fnr, saksblokk, saksnr)
                    call.respondText("{\"result\": \"${res}\"}", ContentType.Application.Json, HttpStatusCode.OK)

                }catch(e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "bad request")
                    return@get
                }
            }
        }
    }
}

fun getPreparedStatementDecisionResult(): PreparedStatement {
    return dbConnection!!.prepareStatement("SELECT S10_RESULTAT as resultat FROM INFOTRYGD_HJQ.SA_SAK_10 WHERE S01_PERSONKEY = ? AND S05_SAKSBLOKK = ? AND S10_SAKSNR = ?")
}

fun queryForDecisionResult(tknr: String, fnr: String, saksblokk: String, saksnr: String): String {
    getPreparedStatementDecisionResult().use {pstmt ->
        pstmt.setString(1, "$tknr$fnr") // S01_PERSONKEY
        pstmt.setString(2, saksblokk) // S05_SAKSBLOKK
        pstmt.setString(3, saksnr) // S10_SAKSNR
        pstmt.executeQuery().use { rs ->
            while (rs.next()) {
                return rs.getString("resultat")
            }
        }
    }
    throw Exception("no such db row")
}
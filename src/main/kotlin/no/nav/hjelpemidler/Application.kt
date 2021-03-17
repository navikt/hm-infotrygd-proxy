package no.nav.hjelpemidler

import com.beust.klaxon.Klaxon
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
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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

    // Serve http REST API requests
    EngineMain.main(args)

    // Cleanup
    logg.info("Cleaning up and stopping.")
    dbConnection!!.close()
}

@ExperimentalTime
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
            post("/vedtak-resultat") {
                try {
                    val reqs = call.receive<Array<VedtakResultatRequest>>()
                    logg.info("Incoming authenticated request for /vedtak-resultat (with parameters: $reqs)")
                    val res = queryForDecisionResult(reqs)
                    call.respondText(Klaxon().toJsonString(res), ContentType.Application.Json, HttpStatusCode.OK)
                }catch(e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "internal server error: $e")
                    return@post
                }
            }
        }
    }
}

fun getPreparedStatementDecisionResult(): PreparedStatement {
    // Filtering for DB_SPLIT = HJ or 99 so that we only look at data that belongs to us
    // even if we are connected to the production db: INFOTRYGD_P
    val query = """
        SELECT S10_RESULTAT
        FROM ${Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_NAME"]}.SA_SAK_10
        WHERE S01_PERSONKEY = ? AND S05_SAKSBLOKK = ? AND S10_SAKSNR = ?
        AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
    """.trimIndent().split("\n").joinToString(" ")

    logg.info("DEBUG: SQL query being prepared: $query")
    return dbConnection!!.prepareStatement(query)
}

data class VedtakResultatRequest(
    val tknr: String,
    val fnr: String,
    val saksblokk: String,
    val saksnr: String,
)

data class VedtakResultatResponse (
    val result: String?,
    val error: String?,
    val queryTimeElapsedMs: Double,
)

@ExperimentalTime
fun queryForDecisionResult(reqs: Array<VedtakResultatRequest>): Array<VedtakResultatResponse> {
    var results = mutableListOf<VedtakResultatResponse>()
    getPreparedStatementDecisionResult().use { pstmt ->
        for (req in reqs) {
            var result: String? = null
            var error: String? = null
            val elapsed: Duration = measureTime {
                pstmt.clearParameters()
                pstmt.setString(1, "${req.tknr}${req.fnr}") // S01_PERSONKEY
                pstmt.setString(2, req.saksblokk) // S05_SAKSBLOKK
                pstmt.setString(3, req.saksnr) // S10_SAKSNR
                pstmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        if (result != null) {
                            error = "we found multiple results for query, this is not supported" // Multiple results not supported
                        }else{
                            result = rs.getString("S10_RESULTAT")
                        }
                    }
                }
            }
            if (result == null) error = "no such decision in the database"
            results.add(VedtakResultatResponse(result, error, elapsed.inMilliseconds))
        }
    }
    return results.toTypedArray()
}
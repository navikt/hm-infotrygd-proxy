package no.nav.hjelpemidler

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import mu.KotlinLogging
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.domain.Fødselsnummer
import no.nav.hjelpemidler.domain.tilInfotrygdFormat
import no.nav.hjelpemidler.metrics.Prometheus
import oracle.jdbc.OracleConnection
import oracle.jdbc.pool.OracleDataSource
import org.slf4j.event.Level
import java.math.BigInteger
import java.security.MessageDigest
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

private var dbConnection: Connection? = null
private val ready = AtomicBoolean(false)

private val dbSkjemaNavn = Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_NAME"]!!

fun main(args: Array<String>) {
    connectToInfotrygdDB()

    // Serve http REST API requests
    EngineMain.main(args)

    // Note: We do not want to set ready=false again, dbConnection.isValid() will have kubernetes restart our entire pod as it is closed below.
    // and setting ready=false forces our /isAlive-endpoint to always say "ALIVE".

    // Cleanup
    logg.info("Cleaning up and stopping.")
    dbConnection?.close()
}

fun connectToInfotrygdDB() {
    // Clean up resources if we have already had a database connection set up here that has now failed
    ready.set(false)
    dbConnection = null

    // Set up a new connection
    try {
        sikkerlogg.info("Connecting to Infotrygd database with db-config-url=${Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_URL"]}, db-config-username=${Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_USR"]}")

        // Set up database connection
        val info = Properties()
        info[OracleConnection.CONNECTION_PROPERTY_USER_NAME] =
            Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_USR"]!!
        info[OracleConnection.CONNECTION_PROPERTY_PASSWORD] =
            Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_PW"]!!
        info[OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH] = "20"

        val ods = OracleDataSource()
        ods.url = Configuration.oracleDatabaseConfig["HM_INFOTRYGD_PROXY_DB_URL"]!!
        ods.connectionProperties = info

        logg.info("Connecting to database")
        dbConnection = ods.connection

        logg.info("Fetching db metadata")
        val dbmd = dbConnection!!.metaData
        logg.info("Driver Name: " + dbmd.driverName)
        logg.info("Driver Version: " + dbmd.driverVersion)

        logg.info("Database connected, hm-infotrygd-proxy ready")
        ready.set(true)

    } catch (e: Exception) {
        logg.info("Exception while connecting to database: $e")
        e.printStackTrace()
        throw e
    }
}

// Meant to fix "java.sql.SQLException: ORA-02399: overskred maks. tilkoblingstid, du blir logget av", by reconnection to the database and retrying
fun <T> withRetryIfDatabaseConnectionIsStale(block: () -> T): T {
    var lastException: SQLException? = null
    for (attempt in 1..3) { // We get three attempts
        try {
            return block() // Success
        } catch (e: SQLException) {
            lastException = e
            if (e.toString().contains("ORA-02399")) {
                logg.warn("Infotrygd replication database closed the connection due to their connection-max-life deadline, we reconnect and try again: $e")
                connectToInfotrygdDB() // Reset database connection
                continue // Retry if we have attempts left
            }
            throw e // Unhandled sql error, we throw up
        }
    }
    throw lastException!! // No more attempts, so we throw the last exception we had
}

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    installAuthentication()

    install(ContentNegotiation) {
        register(
            ContentType.Application.Json,
            JacksonConverter(
                jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            )
        )
    }

    install(CallLogging) {
        level = Level.TRACE
        filter { call ->
            !call.request.path().startsWith("/internal") &&
                    !call.request.path().startsWith("/isalive") &&
                    !call.request.path().startsWith("/isready")
        }
    }

    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            CollectorRegistry.defaultRegistry,
            Clock.SYSTEM
        )
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            LogbackMetrics(),
            KafkaConsumerMetrics(),
        )
    }

    routing {
        // Endpoints for Kubernetes unauthenticated health checks

        get("/isalive") {
            // If we have gotten ready=true we check that dbConnection is still valid, or else we are ALIVE (so we don't get our pod restarted during startup)
            if (ready.get()) {
                val dbValid = dbConnection!!.isValid(10)
                if (!dbValid) {
                    Prometheus.infotrygdDbAvailable.set(0.0)
                    return@get call.respondText("NOT ALIVE", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
                }
                Prometheus.infotrygdDbAvailable.set(1.0)
            }
            call.respondText("ALIVE", ContentType.Text.Plain)
        }

        get("/isready") {
            if (!ready.get()) return@get call.respondText(
                "NOT READY",
                ContentType.Text.Plain,
                HttpStatusCode.ServiceUnavailable
            )
            call.respondText("READY", ContentType.Text.Plain)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()

            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
            }
        }

        // Authenticated database proxy requests
        authenticate("aad") {
            post("/vedtak-resultat") {

                /*
                // For testing infotrygd down-time
                call.respondText("""
                    {"error": "testing down-time measurements"}
                """.trimIndent(), ContentType.Application.Json, HttpStatusCode.InternalServerError)
                return@post
                 */

                try {
                    val reqs = call.receive<Array<VedtakResultatRequest>>()
                    logg.info("Incoming authenticated request for /vedtak-resultat, with ${reqs.size} batch requests")

                    /*
                    for (i in 1..reqs.size) {
                        val req = reqs[i-1]
                        val reqInner = VedtakResultatRequest(req.id, req.tknr, "[MASKED]", req.saksblokk, req.saksnr)
                        logg.info("– [$i/${reqs.size}] $reqInner")
                    }
                     */

                    val res = withRetryIfDatabaseConnectionIsStale {
                        queryForDecisionResult(reqs)
                    }

                    call.respond(res)

                } catch (e: Exception) {
                    logg.error("Exception thrown during processing: $e")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "internal server error: $e")
                    return@post
                }
            }
            post("/har-vedtak-for") {
                try {
                    val req = call.receive<HarVedtakForRequest>()
                    logg.info("Incoming authenticated request for /har-vedtak-for (fnr=MASKED, saksblokk=${req.saksblokk}, saksnr=${req.saksnr}, vedtaksDato=${req.vedtaksDato})")

                    val res = withRetryIfDatabaseConnectionIsStale {
                        queryForDecision(req)
                    }

                    // Allow mocking orderlines from OEBS in dev even with a static infotrygd-database...
                    if (Configuration.application["APPLICATION_PROFILE"]!! == "dev") {
                        res.resultat = true
                    }

                    call.respond(res)

                } catch (e: Exception) {
                    logg.error("Exception thrown during processing: $e")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "internal server error: $e")
                    return@post
                }
            }

            post("/har-vedtak-fra-for") {
                try {
                    val req = call.receive<HarVedtakFraFørRequest>()
                    logg.info("Incoming authenticated request for /har-vedtak-fra-for (fnr=MASKED)")

                    val res = withRetryIfDatabaseConnectionIsStale {
                        queryForHarVedtakFraFør(req)
                    }

                    // Allow mocking orderlines from OEBS in dev even with a static infotrygd-database...
                    if (Configuration.application["APPLICATION_PROFILE"]!! == "dev") {
                        //res.harVedtakFraFør = true
                    }

                    call.respond(res)

                } catch (e: Exception) {
                    logg.error("Exception thrown during processing: $e")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "internal server error: $e")
                    return@post
                }
            }

            post("/hent-saker-for-bruker") {
                try {
                    val req = call.receive<HentSakerForBrukerRequest>()
                    logg.info("Incoming authenticated request for /hent-saker-for-bruker")

                    val res = withRetryIfDatabaseConnectionIsStale {
                        queryForHentSakerForBruker(req)
                    }

                    // Allow mocking orderlines from OEBS in dev even with a static infotrygd-database...
                    //if (Configuration.application["APPLICATION_PROFILE"]!! == "dev") {
                      //  res.resultat = true
                    //}

                    call.respond(res)

                } catch (e: Exception) {
                    logg.error(e){"Feil med henting av saker for bruker"}
                    call.respond(HttpStatusCode.InternalServerError, "Feil med henting av saker for bruker: $e")
                    return@post
                }
            }
        }
    }
}

fun getPreparedStatementDecisionResult(): PreparedStatement {
    // Filtering for DB_SPLIT = HJ or 99 so that we only look at data that belongs to us
    // even if we are connected to the production db: INFOTRYGD_P
    val query =
        """
            SELECT S10_RESULTAT, S10_VEDTAKSDATO, S10_KAPITTELNR, S10_VALG, S10_UNDERVALG, S10_TYPE
            FROM ${dbSkjemaNavn}.SA_SAK_10
            WHERE TK_NR = ? AND F_NR = ? AND S05_SAKSBLOKK = ? AND S10_SAKSNR = ?
            AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
        """.trimIndent().split("\n").joinToString(" ")
    logg.info("DEBUG: SQL query being prepared: $query")
    return dbConnection!!.prepareStatement(query)
}

fun getPreparedStatementDoesPersonkeyExist(): PreparedStatement {
    val query =
        """
            SELECT count(*) AS number_of_rows
            FROM ${dbSkjemaNavn}.SA_SAK_10
            WHERE TK_NR = ? AND F_NR = ?
            AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
        """.trimIndent().split("\n").joinToString(" ")
    logg.info("DEBUG: SQL query being prepared for PersonKeyExists-check: $query")
    return dbConnection!!.prepareStatement(query)
}

fun getPreparedStatementHasDecisionFor(): PreparedStatement {
    // Filtering for DB_SPLIT = HJ or 99 so that we only look at data that belongs to us
    // even if we are connected to the production db: INFOTRYGD_P
    val query =
        """
            SELECT 1
            FROM ${dbSkjemaNavn}.SA_SAK_10
            WHERE S10_VEDTAKSDATO = ? AND F_NR = ? AND S05_SAKSBLOKK = ? AND S10_SAKSNR = ?
            AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
        """.trimIndent().split("\n").joinToString(" ")
    logg.info("DEBUG: SQL query being prepared: $query")
    return dbConnection!!.prepareStatement(query)
}

fun getPreparedStatementHarVedtakFraFør(): PreparedStatement {
    // Filtering for DB_SPLIT = HJ or 99 so that we only look at data that belongs to us
    // even if we are connected to the production db: INFOTRYGD_P
    val query =
        """
            SELECT 1
            FROM ${dbSkjemaNavn}.SA_SAK_10
            WHERE F_NR = ?
            AND S10_RESULTAT <> 'A '
            AND S10_RESULTAT <> 'H '
            AND S10_RESULTAT <> 'HB'
            AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
        """.trimIndent().split("\n").joinToString(" ")
    logg.info("DEBUG: SQL query being prepared: $query")
    return dbConnection!!.prepareStatement(query)
}

fun getPreparedStatementHentSakerForBruker(): PreparedStatement {
    // Filtering for DB_SPLIT = HJ or 99 so that we only look at data that belongs to us
    // even if we are connected to the production db: INFOTRYGD_P
    val query =
        """
            SELECT 
                SA_SAK_10.F_NR, 
                S10_VEDTAKSDATO, 
                S10_RESULTAT, 
                S10_MOTTATTDATO, 
                S10_KAPITTELNR, 
                S10_VALG, 
                S10_UNDERVALG, 
                S10_SAKSNR, 
                SA_SAK_10.S05_SAKSBLOKK,  
                S05_BRUKERID, 
                S20_OPPLYSNING   
            FROM 
                ${dbSkjemaNavn}.SA_SAK_10, 
                ${dbSkjemaNavn}.SA_SAKSBLOKK_05, 
                ${dbSkjemaNavn}.SA_HENDELSE_20
            WHERE 
                SA_SAK_10.F_NR = 48041423897
            AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
            AND SA_SAK_10.S01_PERSONKEY = SA_SAKSBLOKK_05.S01_PERSONKEY 
            AND SA_SAK_10.S01_PERSONKEY = SA_HENDELSE_20.S01_PERSONKEY
        """.trimIndent().split("\n").joinToString(" ")
    logg.info("DEBUG: SQL query being prepared: $query")
    return dbConnection!!.prepareStatement(query)
}

data class VedtakResultatRequest(
    val id: String,
    val tknr: String,
    val fnr: String,
    val saksblokk: String,
    val saksnr: String,
)

data class VedtakResultatResponse(
    val req: VedtakResultatRequest,
    val vedtaksResult: String?,
    val vedtaksDate: LocalDate?,
    val soknadsType: String?,
    val error: String?,
    val queryTimeElapsedMs: Long,
)

data class HarVedtakForRequest(
    val fnr: String,
    val saksblokk: String,
    val saksnr: String,
    val vedtaksDato: LocalDate,
)

data class HarVedtakForResponse(
    var resultat: Boolean,
)

data class HarVedtakFraFørRequest(
    val fnr: String
)

data class HarVedtakFraFørResponse(
    val harVedtakFraFør: Boolean
)

data class HentSakerForBrukerRequest(
    val fnr: String
)

data class SakerForBrukerResponse(
    val mottattDato: LocalDate?,
    val sakGjelder: List<String>,
    val saksblokk: String?,
    val saksnummer: String?,
    val saksbehandler: String?,
    val opplysning: String?,
    val vedtaksResultat: String?,
    val vedtaksDato: LocalDate?,
    val error: String? = null,
)


private val dateFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.YEAR, 4)
    .optionalStart()
    .parseLenient()
    .appendOffset("+HHMMss", "Z")
    .parseStrict()
    .toFormatter()

@ExperimentalTime
fun queryForDecisionResult(reqs: Array<VedtakResultatRequest>): Array<VedtakResultatResponse> {
    val results = mutableListOf<VedtakResultatResponse>()
    getPreparedStatementDecisionResult().use { pstmt ->
        for (req in reqs) {

            // Check if request looks right
            if (req.fnr.strip().length != 11) {
                val error = "error: request with id=${req.id} has a fnr of length: ${req.fnr.length} != 11"
                logg.error(error)
                results.add(
                    VedtakResultatResponse(
                        req,
                        null,
                        null,
                        null,
                        error,
                        0,
                    )
                )
                continue // Skip further handling of this request
            }
            if (req.tknr.strip().length != 4) {
                val error = "error: request with id=${req.id} has a tknr of length: ${req.tknr.strip().length} != 4"
                logg.error(error)
                results.add(
                    VedtakResultatResponse(
                        req,
                        null,
                        null,
                        null,
                        error,
                        0,
                    )
                )

                continue // Skip further handling of this request
            }
            if (req.saksblokk.strip().length !=1) {
                val error = "error: request with id=${req.id} has an saksblokk of length: ${req.saksblokk.length} != 1"
                logg.error(error)
                results.add(
                    VedtakResultatResponse(
                        req,
                        null,
                        null,
                        null,
                        error,
                        0,
                    )
                )
                continue // Skip further handling of this request
            }
            if (req.saksnr.strip().length != 2) {
                val error = "error: request with id=${req.id} has an saksnr of length: ${req.saksnr.length} != 2"
                logg.error(error)
                results.add(
                    VedtakResultatResponse(
                        req,
                        null,
                        null,
                        null,
                        error,
                        0,
                    )
                )
                continue // Skip further handling of this request
            }

            val fnr =
                "${req.fnr.substring(4, 6)}${req.fnr.substring(2, 4)}${req.fnr.substring(0, 2)}${req.fnr.substring(6)}"

            // Look up the request in the Infotrygd replication database
            var foundResult = false
            var vedtaksResult: String? = null
            var vedtaksDate: String? = null
            var soknadsType: String? = null
            var error: String? = null
            val elapsed: Duration = measureTime {
                pstmt.clearParameters()
                pstmt.setString(1, req.tknr)        // TK_NR
                pstmt.setString(2, fnr)             // F_NR
                pstmt.setString(3, req.saksblokk)   // S05_SAKSBLOKK
                pstmt.setString(4, req.saksnr)      // S10_SAKSNR
                pstmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        if (foundResult) {
                            error =
                                "we found multiple results for query, this is not supported" // Multiple results not supported
                            break
                        } else {
                            foundResult = true
                            vedtaksResult = rs.getString("S10_RESULTAT").trim()
                            vedtaksDate = rs.getString("S10_VEDTAKSDATO").trim()
                            soknadsType = listOf(
                                rs.getString("S10_KAPITTELNR"),
                                rs.getString("S10_VALG"),
                                rs.getString("S10_UNDERVALG"),
                                rs.getString("S10_TYPE")
                            ).joinToString("") {
                                var column = it.trim()
                                if (column.length < 2) {
                                    column = "$column "
                                }
                                column
                            }.trimEnd()
                            if (vedtaksDate!!.length == 7) vedtaksDate =
                                "0$vedtaksDate" // leading-zeros are lost in the database due to use of NUMBER(8) as storage column type
                        }
                    }
                }
            }

            if (!foundResult) {
                val hashedIdent = req.fnr.let { input ->
                    val md: MessageDigest = MessageDigest.getInstance("SHA-512")
                    val messageDigest = md.digest(input.toByteArray())
                    // Convert byte array into signum representation
                    val no = BigInteger(1, messageDigest)
                    // Convert message digest into hex value
                    var hashtext: String = no.toString(16)
                    // Add preceding 0s to make it 128 chars long
                    while (hashtext.length < 128) {
                        hashtext = "0$hashtext"
                    }
                    hashtext
                }

                error = "no such vedtak in the database (tknr=${req.tknr}, saksblokk=${req.saksblokk}, saksnr=${req.saksnr}, hashedIdent=${hashedIdent})"

                getPreparedStatementDoesPersonkeyExist().use { pstmt2 ->
                    pstmt2.clearParameters()
                    pstmt2.setString(1, req.tknr)        // TK_NR
                    pstmt2.setString(2, fnr)             // F_NR
                    pstmt2.executeQuery().use { rs ->
                        if (rs.next()) {
                            val numberOfRows = rs.getInt("number_of_rows")
                            if (numberOfRows > 0) error += "; however personKey has rows in the table: #" + numberOfRows
                                .toString()
                        }
                    }
                }
            }

            try {
                var parsedVedtaksDate: LocalDate? = null
                if (vedtaksDate != null && vedtaksDate != "0") parsedVedtaksDate =
                    LocalDate.parse(vedtaksDate!!, dateFormatter)
                if (vedtaksResult != null && vedtaksResult == "  ") vedtaksResult = ""

                results.add(
                    VedtakResultatResponse(
                        req,
                        vedtaksResult,
                        parsedVedtaksDate,
                        soknadsType,
                        error,
                        elapsed.inWholeMilliseconds
                    )
                )

            } catch (e: Exception) {
                val err = "error: could not parse vedtaksDate=$vedtaksDate: $e"
                error = if (error != null) "error: $error; $err" else err

                logg.error(error)
                e.printStackTrace()

                results.add(
                    VedtakResultatResponse(
                        req,
                        vedtaksResult,
                        null,
                        soknadsType,
                        error,
                        elapsed.inWholeMilliseconds
                    )
                )
            }
        }
    }
    return results.toTypedArray()
}

@ExperimentalTime
fun queryForDecision(req: HarVedtakForRequest): HarVedtakForResponse {
    var result = HarVedtakForResponse(false)
    getPreparedStatementHasDecisionFor().use { pstmt ->
        // Check if request looks right
        if (req.fnr.length != 11) logg.error("error: request has a fnr of length: ${req.fnr.length} != 11")
        if (req.saksblokk.length != 1) logg.error("error: request has an saksblokk of length: ${req.saksblokk.length} != 1")
        if (req.saksnr.length != 2) logg.error("error: request has an saksnr of length: ${req.saksnr.length} != 2")

        val vedtaksDato = req.vedtaksDato.format(dateFormatter)
        val fnr =
            "${req.fnr.substring(4, 6)}${req.fnr.substring(2, 4)}${req.fnr.substring(0, 2)}${req.fnr.substring(6)}"

        // Look up the request in the Infotrygd replication database
        pstmt.clearParameters()
        pstmt.setString(1, vedtaksDato)     // S10_VEDTAKSDATO
        pstmt.setString(2, fnr)             // F_NR
        pstmt.setString(3, req.saksblokk)   // S05_SAKSBLOKK
        pstmt.setString(4, req.saksnr)      // S10_SAKSNR
        pstmt.executeQuery().use { rs ->
            if (rs.next()) {
                result = HarVedtakForResponse(true)
            }
        }
    }
    return result
}


@ExperimentalTime
fun queryForHarVedtakFraFør(req: HarVedtakFraFørRequest): HarVedtakFraFørResponse {
    var result = HarVedtakFraFørResponse(false)
    getPreparedStatementHarVedtakFraFør().use { pstmt ->
        // Check if request looks right
        if (req.fnr.length != 11) logg.error("error: request has a fnr of length: ${req.fnr.length} != 11")

        val fnr =
            "${req.fnr.substring(4, 6)}${req.fnr.substring(2, 4)}${req.fnr.substring(0, 2)}${req.fnr.substring(6)}"

        // Look up the request in the Infotrygd replication database
        pstmt.clearParameters()
        pstmt.setString(1, fnr)             // F_NR
        pstmt.executeQuery().use { rs ->
            if (rs.next()) {
                result = HarVedtakFraFørResponse(true)
            }
        }
    }
    return result
}


@ExperimentalTime
fun queryForHentSakerForBruker(req: HentSakerForBrukerRequest): List<SakerForBrukerResponse> {
    val saker: MutableList<SakerForBrukerResponse> = mutableListOf()
    getPreparedStatementHentSakerForBruker().use { pstmt ->
        val fnr = Fødselsnummer(req.fnr)

        pstmt.clearParameters()
        pstmt.setString(1, fnr.tilInfotrygdFormat())
        pstmt.executeQuery().use { rs ->
            if (rs.next()) {
                try {
                    val vedtaksResult = rs.getString("S10_RESULTAT")?.trim()
                    val vedtaksDate = rs.getString("S10_VEDTAKSDATO")?.trim()
                    val mottattDato = rs.getString("S10_MOTTATTDATO")
                    val sakGjelder = listOf<String>(
                        rs.getString("S10_KAPITTELNR"),
                        rs.getString("S10_VALG"),
                        rs.getString("S10_UNDERVALG")
                    )
                    val saksblokk = rs.getString("SA_SAK_10.S05_SAKSBLOKK")
                    val saksNummer = rs.getString("S10_SAKSNR")
                    val brukerID = rs.getString("S05_BRUKERID")
                    val opplysning = rs.getString("S20_OPPLYSNING")

                    val sak = SakerForBrukerResponse(
                        mottattDato = mottattDato?.let { LocalDate.parse(it, dateFormatter) },
                        sakGjelder = sakGjelder,
                        saksblokk = saksblokk,
                        saksnummer = saksNummer,
                        saksbehandler = brukerID,
                        opplysning = opplysning,
                        vedtaksResultat = vedtaksResult,
                        vedtaksDato = vedtaksDate?.let { LocalDate.parse(it, dateFormatter) })

                    saker.add(sak)
                } catch (e: Exception) {
                    logg.error(e){"Feil med parsing av sak for bruker"}
                }
            }
        }
    }
    return saker
}

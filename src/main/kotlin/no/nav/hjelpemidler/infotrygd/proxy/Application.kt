package no.nav.hjelpemidler.infotrygd.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.database.Oracle
import no.nav.hjelpemidler.database.createDataSource
import org.slf4j.event.Level
import java.time.LocalDate

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args)

/**
 * Brukes i application.conf.
 */
@Suppress("unused")
fun Application.module() {
    installAuthentication()
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    install(CallLogging) {
        level = Level.TRACE
        filter { call ->
            val path = call.request.path()
            !(path.startsWith("/internal") || path.startsWith("/isalive") || path.startsWith("/isready"))
        }
    }

    install(MicrometerMetrics) { registry = Prometheus.registry }

    val database = createDataSource(Oracle) {
        jdbcUrl = Configuration.HM_INFOTRYGD_PROXY_DB_JDBC_URL
        username = Configuration.HM_INFOTRYGD_PROXY_DB_USERNAME
        password = Configuration.HM_INFOTRYGD_PROXY_DB_PASSWORD
        databaseName = Configuration.HM_INFOTRYGD_PROXY_DB_NAME
        schema = Configuration.HM_INFOTRYGD_PROXY_DB_NAME
        connectionInitSql = """ALTER SESSION SET CURRENT_SCHEMA = $schema"""
    }.let(::Database)
    environment.monitor.subscribe(ApplicationStopping) {
        database.close()
    }

    val infotrygdService = InfotrygdService(database)

    routing {
        healthCheckApi(database)

        authenticate("aad") {
            post("/vedtak-resultat") {
                // fixme -> eksisterende løsning støtter at noen av requestene er ugyldige og svarer med en response for disse, trenger vi faktisk det?
                val requests = call.receive<List<VedtaksresultatRequest>>()
                val response = infotrygdService.hentVedtaksresultat(requests)
                call.respond(response)
            }

            post("/har-vedtak-for") {
                val request = call.receive<HarVedtakForRequest>()
                val response = infotrygdService.harVedtakFor(request)
                if (Environment.current.tier.isDev) response.resultat = true
                call.respond(response)
            }

            post("/har-vedtak-om-ha") {
                val request = call.receive<HarVedtakOmHøreapparatRequest>()
                val response = infotrygdService.harVedtakOmHøreapparat(request)
                if (Environment.current.tier.isDev) response.resultat = true
                call.respond(response)
            }

            post("/har-vedtak-fra-for") {
                val request = call.receive<HarVedtakFraFørRequest>()
                val response = infotrygdService.harVedtakFraFør(request.fnr)
                call.respond(response)
            }

            post("/hent-brevstatistikk") {
                data class Request(
                    val enhet: String,
                    val minVedtaksdato: LocalDate,
                    val maksVedtaksdato: LocalDate,
                )
                val req = call.receive<Request>()
                val response = infotrygdService.hentBrevstatistikk(req.enhet, req.minVedtaksdato, req.maksVedtaksdato)
                call.respond(response)
            }

            post("/hent-brevstatistikk2") {
                data class Request(
                    val enhet: String,
                    val minVedtaksdato: LocalDate,
                    val maksVedtaksdato: LocalDate,
                    val digitaleOppgaveIder: Set<String>,
                )
                val req = call.receive<Request>()
                val response = infotrygdService.hentBrevstatistikk2(req.enhet, req.minVedtaksdato, req.maksVedtaksdato, req.digitaleOppgaveIder)
                call.respond(response)
            }

            post("/har-sak-med-es-gsak-oppdragsid") {
                data class Request(
                    val id: String,
                )
                data class Response(
                    val harId: Boolean,
                )
                val req = call.receive<Request>()
                val response = infotrygdService.harSakMedEsGsakOppdragsId(req.id)
                call.respond(Response(harId = response))
            }

            // fixme -> slett denne, ser ikke ut som den er i bruk
            post("/hent-saker-for-bruker") {
                val request = call.receive<HentSakerForBrukerRequest>()
                val response = infotrygdService.hentSakerForBruker(request.fnr)
                call.respond(response)
            }
        }
    }
}

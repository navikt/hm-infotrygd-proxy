package no.nav.hjelpemidler.healtcheck

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.hjelpemidler.metrics.Prometheus
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

fun Route.healtcheckApi(ready: AtomicBoolean, dbConnection: Connection?) {
    get("/isalive") {
        // If we have gotten ready=true we check that dbConnection is still valid, or else we are ALIVE (so we don't get our pod restarted during startup)
        if (ready.get()) {
            val dbValid = dbConnection!!.isValid(10)
            if (!dbValid) {
                Prometheus.infotrygdDbAvailable.set(0.0)
                return@get call.respondText("NOT ALIVE", status = HttpStatusCode.ServiceUnavailable)
            }
            Prometheus.infotrygdDbAvailable.set(1.0)
        }
        call.respondText("ALIVE")
    }

    get("/isready") {
        if (!ready.get()) return@get call.respondText(
            "NOT READY", status = HttpStatusCode.ServiceUnavailable
        )
        call.respondText("READY")
    }

    get("/metrics") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()

        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
        }
    }
}
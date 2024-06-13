package no.nav.hjelpemidler.infotrygd.proxy

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.hjelpemidler.infotrygd.proxy.database.Database

fun Route.internalApi(database: Database) {
    get("/isalive") {
        /*
        if (!database.isValid()) {
            Prometheus.infotrygdDbAvailable.set(0.0)
            call.respondText("NOT ALIVE", status = HttpStatusCode.ServiceUnavailable)
        } else {
            Prometheus.infotrygdDbAvailable.set(1.0)
            call.respondText("ALIVE")
        }
         */
        call.respondText("ALIVE")
    }

    get("/isready") {
        call.respondText("READY")
    }

    get("/metrics") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
        }
    }
}

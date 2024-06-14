package no.nav.hjelpemidler.infotrygd.proxy

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.hjelpemidler.infotrygd.proxy.database.Database

fun Route.healthCheckApi(database: Database) {
    get("/isalive") {
        if (!database.isValid()) {
            Prometheus.infotrygdDatabaseAvailable.set(0)
            call.respondText("NOT ALIVE", status = HttpStatusCode.ServiceUnavailable)
        } else {
            Prometheus.infotrygdDatabaseAvailable.set(1)
            call.respondText("ALIVE")
        }
    }

    get("/isready") {
        call.respondText("READY")
    }

    get("/metrics") {
        call.respond(Prometheus.registry.scrape())
    }
}

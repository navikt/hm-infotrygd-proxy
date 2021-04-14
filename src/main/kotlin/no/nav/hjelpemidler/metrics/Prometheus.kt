package no.nav.hjelpemidler.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge

internal object Prometheus {
    val collectorRegistry = CollectorRegistry.defaultRegistry

    val infotrygdDbAvailable = Gauge
        .build()
        .name("hm_infotrygd_proxy_infotrygd_db_available")
        .help("Infotrygd replika-database tilgjengelig")
        .register(collectorRegistry)
}

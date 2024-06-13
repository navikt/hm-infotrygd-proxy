package no.nav.hjelpemidler.infotrygd.proxy

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge

internal object Prometheus {
    private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

    val infotrygdDbAvailable: Gauge = Gauge
        .build()
        .name("hm_infotrygd_proxy_infotrygd_db_available")
        .help("Infotrygd replika-database tilgjengelig")
        .register(collectorRegistry)
}

package no.nav.hjelpemidler.infotrygd.proxy

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicLong

object Prometheus {
    val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val infotrygdDatabaseAvailable: AtomicLong =
        registry.gauge("hm_infotrygd_proxy_infotrygd_database_available", AtomicLong(0))!!
}

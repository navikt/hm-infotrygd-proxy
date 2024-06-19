package no.nav.hjelpemidler.infotrygd.proxy

import no.nav.hjelpemidler.database.QueryParameters
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import java.time.LocalDate

fun QueryParameters.tilInfotrygdformat(): Map<String, Any?> = mapValues { (_, value) ->
    when (value) {
        is Fødselsnummer -> value.tilInfotrygdformat()
        is LocalDate -> value.tilInfotrygdformat()
        else -> value
    }
}

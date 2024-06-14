package no.nav.hjelpemidler.infotrygd.proxy.database

import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import java.time.LocalDate

typealias QueryParameters = Map<String, Any?>

fun QueryParameters.prepare(): Map<String, Any?> = mapValues { (_, value) ->
    when (value) {
        // e.g. Saksblokk | Saksnummer | Trygdekontornummer
        is CharSequence -> value.toString()
        is Fødselsnummer -> value.tilInfotrygdformat()
        is LocalDate -> value.tilInfotrygdformat()
        else -> value
    }
}

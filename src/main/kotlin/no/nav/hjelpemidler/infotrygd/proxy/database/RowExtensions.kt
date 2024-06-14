package no.nav.hjelpemidler.infotrygd.proxy.database

import kotliquery.Row
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksblokk
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Trygdekontornummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.fødselsnummerFraInfotrygd
import java.time.LocalDate

fun Row.toMap(): Map<String, Any?> {
    val metaData = metaDataOrNull()
    return (1..metaData.columnCount).associate { columnIndex ->
        metaData.getColumnName(columnIndex) to anyOrNull(columnIndex)
    }
}

// fixme -> bedre navn
fun Row.localDateOrNull2(columnName: String): LocalDate? {
    val value = stringOrNull(columnName) ?: return null
    if (value.isBlank() || value == "0") return null
    return LocalDate.parse(value.padStart(8, '0'), infotrygdDateTimeFormatter)
}

fun Row.fødelsnummer(columnName: String): Fødselsnummer {
    return fødselsnummerFraInfotrygd(string(columnName))
}

fun Row.saksblokk(columnName: String): Saksblokk {
    return Saksblokk(string(columnName))
}

fun Row.saksnummer(columnName: String): Saksnummer {
    return Saksnummer(string(columnName))
}

fun Row.trygdekontornummer(columnName: String): Trygdekontornummer {
    return Trygdekontornummer(string(columnName))
}

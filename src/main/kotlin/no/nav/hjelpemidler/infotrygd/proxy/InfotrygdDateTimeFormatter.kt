package no.nav.hjelpemidler.infotrygd.proxy

import no.nav.hjelpemidler.database.Row
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

fun LocalDate.tilInfotrygdformat(): String = format(infotrygdDateTimeFormatter)

val infotrygdDateTimeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.YEAR, 4)
    .optionalStart()
    .parseLenient()
    .appendOffset("+HHMMss", "Z")
    .parseStrict()
    .toFormatter()

fun Row.infotrygdDateOrNull(columnName: String): LocalDate? {
    val value = stringOrNull(columnName) ?: return null
    if (value.isBlank() || value == "0") return null
    return LocalDate.parse(value.padStart(8, '0'), infotrygdDateTimeFormatter)
}

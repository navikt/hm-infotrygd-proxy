package no.nav.hjelpemidler.infotrygd.proxy.database

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

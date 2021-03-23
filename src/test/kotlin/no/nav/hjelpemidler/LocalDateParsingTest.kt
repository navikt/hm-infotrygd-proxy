package no.nav.hjelpemidler

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.Month

internal class LocalDateParsingTest {
    @Test
    fun `Parse vedtaksdato to LocalDate`() {
        val vedtaksDate: String? = "22122012"

        val formatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.YEAR, 4)
            .optionalStart()
            .parseLenient()
            .appendOffset("+HHMMss", "Z")
            .parseStrict()
            .toFormatter()

        val parsedVedtaksDate = LocalDate.parse(vedtaksDate!!, formatter)

        assertEquals(2012, parsedVedtaksDate.year)
        assertEquals(Month.DECEMBER, parsedVedtaksDate.month)
        assertEquals(22, parsedVedtaksDate.dayOfMonth)
    }
}
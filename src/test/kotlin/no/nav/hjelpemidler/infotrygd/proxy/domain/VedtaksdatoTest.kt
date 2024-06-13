package no.nav.hjelpemidler.infotrygd.proxy.domain

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class VedtaksdatoTest {
    @Test
    fun `Konverter vedtaksdato til LocalDate`() {
        val vedtaksdato = "22122012"

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

        val parsedVedtaksdato = LocalDate.parse(vedtaksdato, formatter)

        assertEquals(2012, parsedVedtaksdato.year)
        assertEquals(Month.DECEMBER, parsedVedtaksdato.month)
        assertEquals(22, parsedVedtaksdato.dayOfMonth)
    }
}

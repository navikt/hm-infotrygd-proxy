package no.nav.hjelpemidler.infotrygd.proxy.domain

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.infotrygd.proxy.database.infotrygdDateTimeFormatter
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test

class VedtaksdatoTest {
    @Test
    fun `Konverter vedtaksdato til LocalDate`() {
        val vedtaksdato = LocalDate.parse("22122012", infotrygdDateTimeFormatter)

        vedtaksdato.year shouldBe 2012
        vedtaksdato.month shouldBe Month.DECEMBER
        vedtaksdato.dayOfMonth shouldBe 22
    }
}

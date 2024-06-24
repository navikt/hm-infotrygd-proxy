package no.nav.hjelpemidler.infotrygd.proxy.domain

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FødselsnummerTest {
    @Test
    fun `Fnr består kun av tall`() {
        shouldThrow<IllegalArgumentException> { Fødselsnummer("123456789AB") }
    }

    @Test
    fun `Fnr kan ikke inneholde mindre enn 11 tegn`() {
        shouldThrow<IllegalArgumentException> { Fødselsnummer("1234567890") }
    }

    @Test
    fun `Fnr kan ikke inneholde mer enn 11 tegn`() {
        shouldThrow<IllegalArgumentException> { Fødselsnummer("123456789101") }
    }

    @Test
    fun `Fnr må være 11 tegn`() {
        shouldNotThrowAny { Fødselsnummer("12345678910") }
    }

    @Test
    fun `Skal konvertere fnr til Infotrygd-format`() {
        Fødselsnummer("11223345678").tilInfotrygdformat() shouldBe "33221145678"
        fødselsnummerFraInfotrygd("33221145678") shouldBe Fødselsnummer("11223345678")
    }
}

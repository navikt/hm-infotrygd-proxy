package no.nav.hjelpemidler.infotrygd.proxy.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FødselsnummerTest {

    @Test
    fun mustBeNumbersOnly() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            Fødselsnummer("123456789AB")
        }
    }

    @Test
    fun cannotBeLessThanElevenDigits() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            Fødselsnummer("1234567890")
        }
    }

    @Test
    fun cannotBeMoreThanElevenDigits() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            Fødselsnummer("123456789101")
        }
    }

    @Test
    fun mustBeExactlyElevenDigits() {
        Assertions.assertNotNull(Fødselsnummer("12345678910"))
    }

    @Test
    fun mapToInfotrygdFormat() {
        Assertions.assertEquals("33221156789", Fødselsnummer("11223356789").tilInfotrygdFormat())
    }
}

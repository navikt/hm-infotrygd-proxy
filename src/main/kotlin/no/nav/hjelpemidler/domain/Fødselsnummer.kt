package no.nav.hjelpemidler.domain

data class Fødselsnummer(val value: String) {
    private val elevenDigits = Regex("\\d{11}")

    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("$value er ikke gyldig fødselsnummer")
        }
    }
}

/**
 * Infotrygd har snudd om på fødselsdatoen i fødselsnummeret slik at det blir på formen ååmmddxxxxx i stedet for det vanlige ddmmååxxxxx
 */
fun Fødselsnummer.tilInfotrygdFormat(): String =
    "${this.value.substring(4, 6)}${this.value.substring(2, 4)}${this.value.substring(0, 2)}${this.value.substring(6)}"

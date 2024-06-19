package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue
import kotliquery.Row

data class Fødselsnummer(@JsonValue val value: String) {
    init {
        require(value.matches(elevenDigits)) {
            "'$value' er ikke et gyldig fødselsnummer"
        }
    }

    override fun toString(): String = value

    fun tilInfotrygdformat(): String = byttFormat(value)
}

private val elevenDigits: Regex = Regex("\\d{11}")

/**
 * Infotrygd har snudd om på fødselsdatoen i fødselsnummeret slik at det
 * blir på formen ååmmddxxxxx i stedet for det vanlige ddmmååxxxxx.
 */
private fun byttFormat(value: String): String {
    return "${value.substring(4, 6)}${value.substring(2, 4)}${value.substring(0, 2)}${value.substring(6)}"
}

fun fødselsnummerFraInfotrygd(value: String): Fødselsnummer = Fødselsnummer(byttFormat(value))

fun Row.fødelsnummer(columnName: String): Fødselsnummer {
    return fødselsnummerFraInfotrygd(string(columnName))
}

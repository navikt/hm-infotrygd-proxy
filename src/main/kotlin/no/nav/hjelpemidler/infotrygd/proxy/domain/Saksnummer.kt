package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue

data class Saksnummer(@JsonValue val value: String) : CharSequence by value {
    init {
        require(value.trim().length == 2) {
            "'$value' er ikke et gyldig saksnummer"
        }
    }

    override fun toString(): String = value
}

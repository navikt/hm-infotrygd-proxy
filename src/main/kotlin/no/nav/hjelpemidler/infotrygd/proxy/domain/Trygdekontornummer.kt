package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue

data class Trygdekontornummer(@JsonValue val value: String) : CharSequence by value {
    init {
        require(value.trim().length == 4) {
            "'$value' er ikke et gyldig trygdekontornummer"
        }
    }

    override fun toString(): String = value
}

package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue

data class Saksblokk(@JsonValue val value: String) : CharSequence by value {
    init {
        require(value.trim().length == 1) {
            "'$value' er ikke en gyldig saksblokk"
        }
    }

    override fun toString(): String = value
}

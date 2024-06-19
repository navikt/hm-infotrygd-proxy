package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue
import kotliquery.Row
import no.nav.hjelpemidler.database.QueryParameter

data class Saksblokk(@JsonValue override val value: String) : QueryParameter<String> {
    init {
        require(value.trim().length == 1) {
            "'$value' er ikke en gyldig saksblokk"
        }
    }

    override fun toString(): String = value
}

fun Row.saksblokk(columnName: String): Saksblokk = Saksblokk(string(columnName))

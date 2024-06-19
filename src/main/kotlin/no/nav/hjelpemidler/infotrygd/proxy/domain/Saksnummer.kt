package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue
import kotliquery.Row
import no.nav.hjelpemidler.database.QueryParameter

data class Saksnummer(@JsonValue override val value: String) : QueryParameter<String> {
    init {
        require(value.trim().length == 2) {
            "'$value' er ikke et gyldig saksnummer"
        }
    }

    override fun toString(): String = value
}

fun Row.saksnummer(columnName: String): Saksnummer = Saksnummer(string(columnName))

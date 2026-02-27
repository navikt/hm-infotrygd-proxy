package no.nav.hjelpemidler.infotrygd.proxy.domain

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.hjelpemidler.database.Row
import no.nav.hjelpemidler.domain.ValueType

data class Trygdekontornummer(@JsonValue override val value: String) : ValueType<String> {
    init {
        require(value.trim().length == 4) {
            "'$value' er ikke et gyldig trygdekontornummer"
        }
    }

    override fun toString(): String = value
}

fun Row.trygdekontornummer(columnName: String): Trygdekontornummer = Trygdekontornummer(string(columnName))

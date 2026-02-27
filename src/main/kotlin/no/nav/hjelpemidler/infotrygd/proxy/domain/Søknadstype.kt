package no.nav.hjelpemidler.infotrygd.proxy.domain

import no.nav.hjelpemidler.database.Row

/**
 * Også kalt stønadsklassifisering.
 */
data class Søknadstype(
    val kapittelnr: String,
    val valg: String,
    val undervalg: String,
    val type: String,
) {
    override fun toString(): String = listOf(
        kapittelnr,
        valg,
        undervalg,
        type,
    ).joinToString("") { it.trim().padEnd(2, ' ') }.trim()
}

fun Row.tilSøknadstype(): Søknadstype = Søknadstype(
    stringOrNull("S10_KAPITTELNR") ?: "",
    stringOrNull("S10_VALG") ?: "",
    stringOrNull("S10_UNDERVALG") ?: "",
    stringOrNull("S10_TYPE") ?: "",
)

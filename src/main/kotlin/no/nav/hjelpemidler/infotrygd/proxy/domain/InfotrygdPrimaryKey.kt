package no.nav.hjelpemidler.infotrygd.proxy.domain

import no.nav.hjelpemidler.infotrygd.proxy.tilInfotrygdformat

data class InfotrygdPrimaryKey(
    val fnr: Fødselsnummer,
    val trygdekontornummer: Trygdekontornummer,
    val saksblokk: Saksblokk,
    val saksnummer: Saksnummer,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "fnr" to fnr,
        "tknr" to trygdekontornummer,
        "saksblokk" to saksblokk,
        "saksnr" to saksnummer,
    ).tilInfotrygdformat()
}

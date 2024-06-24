package no.nav.hjelpemidler.infotrygd.proxy

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksblokk
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Trygdekontornummer
import java.time.LocalDate

data class VedtaksresultatRequest(
    @JsonProperty("id")
    val søknadId: String, // UUID
    val fnr: Fødselsnummer,
    val tknr: Trygdekontornummer,
    val saksblokk: Saksblokk,
    val saksnr: Saksnummer,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to søknadId,
        "fnr" to fnr,
        "tknr" to tknr,
        "saksblokk" to saksblokk,
        "saksnr" to saksnr,
    ).tilInfotrygdformat()
}

data class VedtaksresultatResponse(
    @JsonProperty("req")
    val request: VedtaksresultatRequest,
    @JsonProperty("vedtaksResult")
    val vedtaksresultat: String?,
    @JsonProperty("vedtaksDate")
    val vedtaksdato: LocalDate?,
    @JsonProperty("soknadsType")
    val søknadstype: String?,
    val feilkode: Feilkode? = null,
    @JsonIgnore
    val antall: Long? = null,
    val queryTimeElapsedMs: Long = -1,
) {
    @JsonIgnore
    constructor(
        request: VedtaksresultatRequest,
        feilkode: Feilkode,
        antall: Long? = null,
        queryTimeElapsedMs: Long = -1,
    ) : this(
        request = request,
        vedtaksresultat = null,
        vedtaksdato = null,
        søknadstype = null,
        feilkode = feilkode,
        antall = antall,
        queryTimeElapsedMs = queryTimeElapsedMs,
    )

    @Suppress("unused")
    val feilmelding: String?
        @JsonProperty("error")
        get() = when (feilkode) {
            Feilkode.VEDTAKSRESULTAT_IKKE_FUNNET ->
                "Fant ikke vedtaksresultat, tknr: ${request.tknr}, saksblokk: ${request.saksblokk}, saksnr: ${request.saksnr}"

            Feilkode.VEDTAKSRESULTAT_IKKE_FUNNET_HAR_ANDRE_SAKER ->
                "Fant ikke vedtaksresultat, men bruker har andre saker, tknr: ${request.tknr}, saksblokk: ${request.saksblokk}, saksnr: ${request.saksnr}, antall: $antall"

            else -> null
        }

    enum class Feilkode {
        VEDTAKSRESULTAT_IKKE_FUNNET,
        VEDTAKSRESULTAT_IKKE_FUNNET_HAR_ANDRE_SAKER,
    }
}

data class HarVedtakForRequest(
    val fnr: Fødselsnummer,
    @JsonAlias("vedtaksDato")
    val vedtaksdato: LocalDate,
    val saksblokk: Saksblokk,
    val saksnr: Saksnummer,
)

data class HarVedtakForResponse(
    var resultat: Boolean,
)

data class HarVedtakFraFørRequest(
    val fnr: Fødselsnummer,
)

data class HarVedtakFraFørResponse(
    val harVedtakFraFør: Boolean,
)

data class HentSakerForBrukerRequest(
    val fnr: Fødselsnummer,
)

data class HentSakerForBrukerResponse(
    val mottattDato: LocalDate?,
    val søknadstype: String?,
    val saksblokk: Saksblokk?,
    val saksnummer: Saksnummer?,
    val vedtaksresultat: String?,
    val vedtaksdato: LocalDate?,
    val saksbehandler: String?,
    val opplysning: String?,
)

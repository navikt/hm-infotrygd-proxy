package no.nav.hjelpemidler.infotrygd.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer

private val log = KotlinLogging.logger {}

class InfotrygdService(private val database: Database) {
    suspend fun hentVedtaksresultat(requests: List<VedtaksresultatRequest>): List<VedtaksresultatResponse> {
        if (requests.isEmpty()) return emptyList()
        log.info { "Henter vedtaksresultat, antall spørringer: ${requests.size}" }
        return database.transaction {
            infotrygdDao.hentVedtaksresultat(requests)
        }
    }

    suspend fun harVedtakFor(request: HarVedtakForRequest): HarVedtakForResponse {
        log.info { "Sjekker om bruker har vedtak, vedtaksdato: ${request.vedtaksdato}, saksblokk: ${request.saksblokk}, saksnr: ${request.saksnr}" }
        return database.transaction {
            val resultat = infotrygdDao.harVedtakFor(
                request.fnr,
                request.vedtaksdato,
                request.saksblokk,
                request.saksnr,
            )
            HarVedtakForResponse(resultat)
        }
    }

    suspend fun harVedtakFraFør(fnr: Fødselsnummer): HarVedtakFraFørResponse {
        log.info { "Sjekker om bruker har vedtak fra før" }
        return database.transaction {
            val harVedtakFraFør = infotrygdDao.harVedtakFraFør(fnr)
            HarVedtakFraFørResponse(harVedtakFraFør)
        }
    }

    suspend fun hentSakerForBruker(fnr: Fødselsnummer): List<HentSakerForBrukerResponse> {
        log.info { "Henter saker for bruker" }
        return database.transaction {
            infotrygdDao.hentSakerForBruker(fnr)
        }
    }
}

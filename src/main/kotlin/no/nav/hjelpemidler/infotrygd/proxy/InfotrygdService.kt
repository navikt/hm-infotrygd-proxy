package no.nav.hjelpemidler.infotrygd.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import java.time.LocalDate

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

    suspend fun harVedtakOmHøreapparat(request: HarVedtakOmHøreapparatRequest): HarVedtakOmHøreapparatResponse {
        log.info { "Sjekker om bruker har vedtak om høreapparat" }
        return database.transaction {
            val resultat = infotrygdDao.harVedtakOmHøreapparat(
                request.fnr,
            )
            HarVedtakOmHøreapparatResponse(resultat)
        }
    }

    suspend fun harVedtakFraFør(fnr: Fødselsnummer): HarVedtakFraFørResponse {
        log.info { "Sjekker om bruker har vedtak fra før" }
        return database.transaction {
            val harVedtakFraFør = infotrygdDao.harVedtakFraFør(fnr)
            HarVedtakFraFørResponse(harVedtakFraFør)
        }
    }

    suspend fun hentBrevstatistikk(enhet: String, minVedtaksdato: LocalDate, maksVedtaksdato: LocalDate): List<Map<String, Any>> {
        log.info { "Henter brevstatistikk for $enhet (minVedtaksdato: $minVedtaksdato, maksVedtaksdato: $maksVedtaksdato)" }
        return database.transaction {
            infotrygdDao.hentBrevstatistikk(enhet, minVedtaksdato, maksVedtaksdato)
        }
    }

    suspend fun hentBrevstatistikk2(enhet: String, minVedtaksdato: LocalDate, maksVedtaksdato: LocalDate, digitaleOppgaveIder: Set<String>): List<Map<String, Any>> {
        log.info { "Henter brevstatistikk for $enhet (minVedtaksdato: $minVedtaksdato, maksVedtaksdato: $maksVedtaksdato, len(digitaleOppgaveIder): ${digitaleOppgaveIder.size})" }
        return database.transaction {
            infotrygdDao.hentBrevstatistikk2(enhet, minVedtaksdato, maksVedtaksdato, digitaleOppgaveIder)
        }
    }

    suspend fun harSakMedEsGsakOppdragsId(id: String): Boolean {
        log.info { "Sjekker om harSakMedEsGsakOppdragsId: id: $id" }
        return database.transaction {
            infotrygdDao.harSakMedEsGsakOppdragsId(id)
        }
    }

    suspend fun hentSakerForBruker(fnr: Fødselsnummer): List<HentSakerForBrukerResponse> {
        log.info { "Henter saker for bruker" }
        return database.transaction {
            infotrygdDao.hentSakerForBruker(fnr)
        }
    }
}

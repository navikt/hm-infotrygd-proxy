package no.nav.hjelpemidler.infotrygd.proxy

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.hjelpemidler.infotrygd.proxy.database.testTransaction
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksblokk
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Trygdekontornummer
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class InfotrygdDaoTest {
    private val fnr = Fødselsnummer("11081672972")
    private val saksblokk = Saksblokk("A")
    private val saksnr = Saksnummer("01")

    @Test
    fun `Hent vedtaksresultat`() = runTest {
        val responses = testTransaction {
            infotrygdDao.hentVedtaksresultat(
                listOf(
                    VedtaksresultatRequest(
                        søknadId = UUID.randomUUID().toString(),
                        fnr = fnr,
                        tknr = Trygdekontornummer("1833"),
                        saksblokk = saksblokk,
                        saksnr = saksnr,
                    ),
                ),
            )
        }

        responses shouldHaveSize 1
    }

    @Test
    fun `Har vedtak for`() = runTest {
        val harVedtakFor = testTransaction {
            infotrygdDao.harVedtakFor(fnr, LocalDate.parse("2021-01-15"), saksblokk, saksnr)
        }

        harVedtakFor shouldBe true
    }

    @Test
    fun `Har vedtak fra før`() = runTest {
        val harVedtakFraFør = testTransaction {
            infotrygdDao.harVedtakFraFør(fnr)
        }

        harVedtakFraFør shouldBe true
    }

    @Test
    fun `Hent saker for bruker`() = runTest {
        val responses = testTransaction {
            infotrygdDao.hentSakerForBruker(fnr)
        }

        responses shouldHaveSize 5
    }
}

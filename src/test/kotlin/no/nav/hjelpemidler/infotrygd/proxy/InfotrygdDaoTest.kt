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
import kotlin.test.Ignore
import kotlin.test.Test

class InfotrygdDaoTest {
    private val fnr = Fødselsnummer("11081672972")
    private val saksblokk = Saksblokk("A")
    private val saksnr = Saksnummer("01")

    @Test
    @Ignore("Fungerer ikke med H2")
    fun `Hent vedtaksresultat`() {
        runTest {
            testTransaction {
                infotrygdDao.hentVedtaksresultat(
                    listOf(
                        VedtaksresultatRequest(
                            søknadId = UUID.randomUUID().toString(),
                            fnr = fnr,
                            tknr = Trygdekontornummer("9999"),
                            saksblokk = saksblokk,
                            saksnr = saksnr,
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun `Har vedtak for`() = runTest {
        testTransaction {
            infotrygdDao.harVedtakFor(fnr, LocalDate.parse("2021-01-15"), saksblokk, saksnr)
        } shouldBe true
    }

    @Test
    fun `Har vedtak fra før`() = runTest {
        testTransaction {
            infotrygdDao.harVedtakFraFør(fnr)
        } shouldBe true
    }

    @Test
    fun `Hent saker for bruker`() = runTest {
        testTransaction {
            infotrygdDao.hentSakerForBruker(fnr)
        } shouldHaveSize 5
    }
}

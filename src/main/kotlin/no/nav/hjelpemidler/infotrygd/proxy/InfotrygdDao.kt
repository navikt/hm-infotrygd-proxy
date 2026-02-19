package no.nav.hjelpemidler.infotrygd.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.configuration.TestEnvironment
import no.nav.hjelpemidler.database.JdbcOperations
import no.nav.hjelpemidler.infotrygd.proxy.domain.Fødselsnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksblokk
import no.nav.hjelpemidler.infotrygd.proxy.domain.Saksnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.fødelsnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.saksblokk
import no.nav.hjelpemidler.infotrygd.proxy.domain.saksnummer
import no.nav.hjelpemidler.infotrygd.proxy.domain.tilSøknadstype
import no.nav.hjelpemidler.infotrygd.proxy.domain.trygdekontornummer
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * NB! Alle spørringer er filtrert på (DB_SPLITT = 'HJ' OR DB_SPLITT = '99') slik at
 * vi kun henter data som tilhører oss, også når vi er tilkoblet produksjonsdatabasen.
 */
class InfotrygdDao(private val tx: JdbcOperations) {
    fun hentVedtaksresultat(requests: List<VedtaksresultatRequest>): List<VedtaksresultatResponse> {
        if (requests.isEmpty()) return emptyList()
        val temporaryTableName = TemporaryTableName("HENT_VEDTAKSRESULTAT")
        if (Environment.current != TestEnvironment) {
            // Oppretter og lagrer innslag i temporærtabell for å slippe dynamisk IN-clause eller flere kall mot databasen.
            // Tabellen eksisterer i minne kun for denne transaksjonen.
            tx.execute(
                """
                CREATE PRIVATE TEMPORARY TABLE $temporaryTableName
                (
                    ID            VARCHAR2(36),
                    F_NR          VARCHAR2(11),
                    TK_NR         VARCHAR2(4),
                    S05_SAKSBLOKK CHAR(1),
                    S10_SAKSNR    CHAR(2)
                ) ON COMMIT DROP DEFINITION
                """.trimIndent(),
            )
        }
        tx.batch(
            """
                INSERT INTO $temporaryTableName (ID, F_NR, TK_NR, S05_SAKSBLOKK, S10_SAKSNR)
                VALUES (:id, :fnr, :tknr, :saksblokk, :saksnr)
            """.trimIndent(),
            requests,
            VedtaksresultatRequest::toMap,
        )
        val responses = tx.list(
            """
                SELECT hent_vedtaksresultat.ID,
                       hent_vedtaksresultat.F_NR,
                       hent_vedtaksresultat.TK_NR,
                       hent_vedtaksresultat.S05_SAKSBLOKK,
                       hent_vedtaksresultat.S10_SAKSNR,
                       sak.ID_SAK,
                       sak.S10_RESULTAT,
                       sak.S10_VEDTAKSDATO,
                       sak.S10_KAPITTELNR,
                       sak.S10_VALG,
                       sak.S10_UNDERVALG,
                       sak.S10_TYPE
                FROM $temporaryTableName hent_vedtaksresultat
                         LEFT JOIN SA_SAK_10 sak ON sak.F_NR = hent_vedtaksresultat.F_NR
                    AND sak.TK_NR = hent_vedtaksresultat.TK_NR
                    AND sak.S05_SAKSBLOKK = hent_vedtaksresultat.S05_SAKSBLOKK
                    AND sak.S10_SAKSNR = hent_vedtaksresultat.S10_SAKSNR
                WHERE (sak.ID_SAK IS NULL OR sak.DB_SPLITT = 'HJ' OR sak.DB_SPLITT = '99')
            """.trimIndent(),
        ) { row ->
            val request = VedtaksresultatRequest(
                søknadId = row.string("ID"),
                fnr = row.fødelsnummer("F_NR"),
                tknr = row.trygdekontornummer("TK_NR"),
                saksblokk = row.saksblokk("S05_SAKSBLOKK"),
                saksnr = row.saksnummer("S10_SAKSNR"),
            )
            val sakId = row.longOrNull("ID_SAK")
            if (sakId == null) {
                VedtaksresultatResponse(
                    request = request,
                    feilkode = VedtaksresultatResponse.Feilkode.VEDTAKSRESULTAT_IKKE_FUNNET,
                )
            } else {
                VedtaksresultatResponse(
                    request = request,
                    vedtaksresultat = row.string("S10_RESULTAT").trim(),
                    vedtaksdato = row.infotrygdDateOrNull("S10_VEDTAKSDATO"),
                    søknadstype = row.tilSøknadstype().toString(),
                )
            }
        }
        val antallByRequest = tx
            .list(
                """
                    SELECT hent_vedtaksresultat.ID,
                           COUNT(1) AS ANTALL
                    FROM $temporaryTableName hent_vedtaksresultat
                             INNER JOIN SA_SAK_10 sak ON sak.F_NR = hent_vedtaksresultat.F_NR
                        AND sak.TK_NR = hent_vedtaksresultat.TK_NR
                    WHERE (sak.DB_SPLITT = 'HJ' OR sak.DB_SPLITT = '99')
                    GROUP BY hent_vedtaksresultat.ID
                """.trimIndent(),
            ) { row -> row.string("ID") to row.long("ANTALL") }
            .toMap()

        return responses.map { response ->
            when (response.feilkode) {
                VedtaksresultatResponse.Feilkode.VEDTAKSRESULTAT_IKKE_FUNNET -> {
                    val antall = antallByRequest[response.request.søknadId] ?: 0
                    if (antall > 0) {
                        response.copy(
                            feilkode = VedtaksresultatResponse.Feilkode.VEDTAKSRESULTAT_IKKE_FUNNET_HAR_ANDRE_SAKER,
                            antall = antall,
                        )
                    } else {
                        response
                    }
                }

                else -> response
            }
        }
    }

    fun harVedtakFor(fnr: Fødselsnummer, vedtaksdato: LocalDate, saksblokk: Saksblokk, saksnr: Saksnummer): Boolean {
        return tx.singleOrNull(
            """
                SELECT 1
                FROM SA_SAK_10
                WHERE F_NR = :fnr
                  AND S10_VEDTAKSDATO = :vedtaksdato
                  AND S05_SAKSBLOKK = :saksblokk
                  AND S10_SAKSNR = :saksnr
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
                FETCH FIRST 1 ROWS ONLY
            """.trimIndent(),
            mapOf(
                "fnr" to fnr,
                "vedtaksdato" to vedtaksdato,
                "saksblokk" to saksblokk,
                "saksnr" to saksnr,
            ).tilInfotrygdformat(),
        ) { true } == true
    }

    fun harVedtakOmHøreapparat(fnr: Fødselsnummer): Boolean {
        return tx.singleOrNull(
            """
                SELECT 1
                FROM SA_SAK_10
                WHERE F_NR = :fnr
                  AND S10_VALG = 'HØ'
                  AND (S10_UNDERVALG = 'DA' OR S10_UNDERVALG = 'UL') -- DA og UL er begge Dagliglivet
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
                FETCH FIRST 1 ROWS ONLY
            """.trimIndent(),
            mapOf(
                "fnr" to fnr,
            ).tilInfotrygdformat(),
        ) { true } == true
    }

    fun harVedtakFraFør(fnr: Fødselsnummer): Boolean {
        return tx.singleOrNull(
            """
                SELECT 1
                FROM SA_SAK_10
                WHERE F_NR = :fnr
                  AND S10_RESULTAT <> 'A '
                  AND S10_RESULTAT <> 'H '
                  AND S10_RESULTAT <> 'HB'
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
                FETCH FIRST 1 ROWS ONLY
            """.trimIndent(),
            mapOf("fnr" to fnr).tilInfotrygdformat(),
        ) { true } == true
    }

    fun hentBrevstatistikk(enhet: String, minVedtaksdato: LocalDate, maksVedtaksdato: LocalDate): List<Map<String, Any>> {
        return tx.list(
            """
                SELECT
                    S10.S10_BEHEN_ENHET,
                    substr(S10.S10_VEDTAKSDATO, -4,4) as AAR,
                    substr(S10.S10_VEDTAKSDATO, -6,2) as MAANED,
                    substr(S10.S10_VEDTAKSDATO, -8,2) as DAG,
                    S20.S20_TEKSTKODE_1 as Brevkode,
                    S10.S10_VALG,
                    S10.S10_UNDERVALG,
                    S10.S10_TYPE,
                    S10.S10_RESULTAT,
                    count(*) as ANTALL
                FROM SA_SAK_10 S10, SA_HENDELSE_20 S20
                WHERE
                    S10.S10_KAPITTELNR    = 'HJ'
                    AND S10.S10_BEHEN_ENHET   = :enhet
                    AND TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY') <= TO_DATE(:maksDato, 'DDMMYYYY')
                    AND TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY') >= TO_DATE(:minDato, 'DDMMYYYY')
                    AND S20.S01_PERSONKEY     = S10.S01_PERSONKEY
                    AND S20.S05_SAKSBLOKK     = S10.S05_SAKSBLOKK
                    AND S20.S20_SAKSNR        = S10.S10_SAKSNR
                    AND S20.S20_TEKSTKODE_1  <> '    '
                GROUP BY S10.S10_BEHEN_ENHET, substr(S10.S10_VEDTAKSDATO, -4,4), substr(S10.S10_VEDTAKSDATO, -6,2), substr(S10.S10_VEDTAKSDATO, -8,2), S20.S20_TEKSTKODE_1, S10.S10_VALG, S10.S10_UNDERVALG, S10.S10_TYPE, S10.S10_RESULTAT
                ORDER BY S10.S10_BEHEN_ENHET, substr(S10.S10_VEDTAKSDATO, -4,4), substr(S10.S10_VEDTAKSDATO, -6,2), substr(S10.S10_VEDTAKSDATO, -8,2), S20.S20_TEKSTKODE_1, S10.S10_VALG, S10.S10_UNDERVALG, S10.S10_TYPE, S10.S10_RESULTAT
            """.trimIndent(),
            mapOf(
                "enhet" to enhet,
                "maksDato" to maksVedtaksdato,
                "minDato" to minVedtaksdato,
            ).tilInfotrygdformat(),
        ) { row ->
            mapOf(
                "enhet" to row.string("S10_BEHEN_ENHET"),
                "år" to row.string("AAR"),
                "måned" to row.string("MAANED"),
                "dag" to row.string("DAG"),
                "brevkode" to row.string("Brevkode"),
                "valg" to row.string("S10_VALG"),
                "undervalg" to row.string("S10_UNDERVALG"),
                "type" to row.string("S10_TYPE"),
                "resultat" to row.string("S10_RESULTAT"),
                "antall" to row.int("ANTALL"),
            )
        }
    }

    fun hentBrevstatistikk2(enhet: String, minVedtaksdato: LocalDate, maksVedtaksdato: LocalDate, digitaleOppgaveIder: Set<String>): List<Map<String, Any>> {
        val temporaryTableName = TemporaryTableName("HENT_BREVSTATISTIKK")
        if (Environment.current != TestEnvironment) {
            // Oppretter og lagrer innslag i temporærtabell for å slippe dynamisk IN-clause eller flere kall mot databasen.
            // Tabellen eksisterer i minne kun for denne transaksjonen.
            tx.execute(
                """
                CREATE PRIVATE TEMPORARY TABLE $temporaryTableName
                (
                    OPPGAVE_ID NUMBER(10),
                    FOUNDIT NUMBER(1)
                ) ON COMMIT DROP DEFINITION
                """.trimIndent(),
            )
        }
        tx.batch(
            """
                INSERT INTO $temporaryTableName (OPPGAVE_ID, FOUNDIT)
                VALUES (:oppgaveId, 1)
            """.trimIndent(),
            digitaleOppgaveIder,
            { oppgaveId ->
                mapOf("oppgaveId" to oppgaveId)
            },
        )
        return tx.list(
            """
                SELECT
                    S10.S10_BEHEN_ENHET,
                    TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY') as DATO,
                    (CASE WHEN OIDER.FOUNDIT = 1 THEN 1 ELSE 0 END) as DIGITAL,
                    S20.S20_TEKSTKODE_1 as BREVKODE,
                    S10.S10_VALG,
                    S10.S10_UNDERVALG,
                    S10.S10_TYPE,
                    S10.S10_RESULTAT,
                    count(*) as ANTALL
                FROM SA_SAK_10 S10
                CROSS JOIN SA_HENDELSE_20 S20
                LEFT JOIN $temporaryTableName OIDER ON S10.S10_ES_GSAK_OPPDRAGSID = OIDER.OPPGAVE_ID
                WHERE
                    S10.S10_KAPITTELNR    = 'HJ'
                    AND S10.S10_BEHEN_ENHET   = :enhet
                    AND TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY') <= TO_DATE(:maksDato, 'DDMMYYYY')
                    AND TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY') >= TO_DATE(:minDato, 'DDMMYYYY')
                    AND S20.S01_PERSONKEY     = S10.S01_PERSONKEY
                    AND S20.S05_SAKSBLOKK     = S10.S05_SAKSBLOKK
                    AND S20.S20_SAKSNR        = S10.S10_SAKSNR
                    AND S20.S20_TEKSTKODE_1  <> '    '
                GROUP BY S10.S10_BEHEN_ENHET, TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY'), CASE WHEN OIDER.FOUNDIT = 1 THEN 1 ELSE 0 END, S20.S20_TEKSTKODE_1, S10.S10_VALG, S10.S10_UNDERVALG, S10.S10_TYPE, S10.S10_RESULTAT
                ORDER BY S10.S10_BEHEN_ENHET, TO_DATE(S10.S10_VEDTAKSDATO DEFAULT '01011900' ON CONVERSION ERROR, 'DDMMYYYY'), CASE WHEN OIDER.FOUNDIT = 1 THEN 1 ELSE 0 END, S20.S20_TEKSTKODE_1, S10.S10_VALG, S10.S10_UNDERVALG, S10.S10_TYPE, S10.S10_RESULTAT
            """.trimIndent(),
            mapOf(
                "enhet" to enhet,
                "maksDato" to maksVedtaksdato,
                "minDato" to minVedtaksdato,
            ).tilInfotrygdformat(),
        ) { row ->
            mapOf(
                "enhet" to row.string("S10_BEHEN_ENHET"),
                "dato" to row.localDate("DATO"),
                "digital" to row.boolean("DIGITAL"),
                "brevkode" to row.string("BREVKODE"),
                "valg" to row.string("S10_VALG"),
                "undervalg" to row.string("S10_UNDERVALG"),
                "type" to row.string("S10_TYPE"),
                "resultat" to row.string("S10_RESULTAT"),
                "antall" to row.int("ANTALL"),
            )
        }
    }

    fun hentSakerForBruker(fnr: Fødselsnummer): List<HentSakerForBrukerResponse> {
        // fixme -> skriv om til inner join (eller slett om den ikke brukes)
        return tx.list(
            """
                SELECT sak.S10_MOTTATTDATO,
                       sak.S10_KAPITTELNR,
                       sak.S10_VALG,
                       sak.S10_UNDERVALG,
                       sak.S10_TYPE,
                       sak.S05_SAKSBLOKK,
                       sak.S10_SAKSNR,
                       sak.S10_RESULTAT,
                       sak.S10_VEDTAKSDATO,
                       saksblokk.S05_BRUKERID,
                       hendelse.S20_OPPLYSNING
                FROM SA_SAK_10 sak,
                     SA_SAKSBLOKK_05 saksblokk,
                     SA_HENDELSE_20 hendelse
                WHERE sak.F_NR = :fnr
                  AND sak.S01_PERSONKEY = saksblokk.S01_PERSONKEY
                  AND sak.S01_PERSONKEY = hendelse.S01_PERSONKEY
                  AND (sak.DB_SPLITT = 'HJ' OR sak.DB_SPLITT = '99')
            """.trimIndent(),
            mapOf("fnr" to fnr).tilInfotrygdformat(),
        ) { row ->
            HentSakerForBrukerResponse(
                mottattDato = row.infotrygdDateOrNull("S10_MOTTATTDATO"),
                søknadstype = row.tilSøknadstype().toString(),
                saksblokk = row.saksblokk("S05_SAKSBLOKK"),
                saksnummer = row.saksnummer("S10_SAKSNR"),
                vedtaksresultat = row.string("S10_RESULTAT").trim(),
                vedtaksdato = row.infotrygdDateOrNull("S10_VEDTAKSDATO"),
                saksbehandler = row.stringOrNull("S05_BRUKERID")?.trim(),
                opplysning = row.stringOrNull("S20_OPPLYSNING")?.trim(),
            )
        }
    }
}

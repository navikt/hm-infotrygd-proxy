package no.nav.hjelpemidler.infotrygd.proxy

import no.nav.hjelpemidler.infotrygd.proxy.database.JdbcOperations

class InfotrygdDao(private val jdbcOperations: JdbcOperations) {
    fun hentVedtaksresultat(fnr: String, tknr: String, saksblokk: String, saksnr: String) {
        jdbcOperations.list(
            """
                SELECT S10_RESULTAT,
                       S10_VEDTAKSDATO,
                       S10_KAPITTELNR,
                       S10_VALG,
                       S10_UNDERVALG,
                       S10_TYPE
                FROM SA_SAK_10
                WHERE TK_NR = ?
                  AND F_NR = ?
                  AND S05_SAKSBLOKK = ?
                  AND S10_SAKSNR = ?
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
            """.trimIndent(),
            tknr,
            fnr,
            saksblokk,
            saksnr,
        ) {}
    }

    fun antall(fnr: String, tknr: String) {
        jdbcOperations.list(
            """
                SELECT COUNT(1) AS antall
                FROM SA_SAK_10
                WHERE TK_NR = ?
                  AND F_NR = ?
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
            """.trimIndent(),
            tknr,
            fnr,
        ) {}
    }

    fun harVedtakFor(fnr: String, vedtaksdato: String, saksblokk: String, saksnr: String) {
        jdbcOperations.list(
            """
                SELECT 1
                FROM SA_SAK_10
                WHERE S10_VEDTAKSDATO = ?
                  AND F_NR = ?
                  AND S05_SAKSBLOKK = ?
                  AND S10_SAKSNR = ?
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
            """.trimIndent(),
            vedtaksdato,
            fnr,
            saksblokk,
            saksnr,
        ) {}
    }

    fun harVedtakFraFør(fnr: String) {
        jdbcOperations.list(
            """
                SELECT 1
                FROM SA_SAK_10
                WHERE F_NR = ?
                  AND S10_RESULTAT <> 'A '
                  AND S10_RESULTAT <> 'H '
                  AND S10_RESULTAT <> 'HB'
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
            """.trimIndent(),
            fnr,
        ) {}
    }

    fun hentSakerForBruker(fnr: String) {
        jdbcOperations.list(
            """
                SELECT SA_SAK_10.F_NR,
                       S10_VEDTAKSDATO,
                       S10_RESULTAT,
                       S10_MOTTATTDATO,
                       S10_KAPITTELNR,
                       S10_VALG,
                       S10_UNDERVALG,
                       S10_SAKSNR,
                       SA_SAK_10.S05_SAKSBLOKK,
                       S05_BRUKERID,
                       S20_OPPLYSNING
                FROM SA_SAK_10,
                     SA_SAKSBLOKK_05,
                     SA_HENDELSE_20
                WHERE SA_SAK_10.F_NR = ?
                  AND (DB_SPLITT = 'HJ' OR DB_SPLITT = '99')
                  AND SA_SAK_10.S01_PERSONKEY = SA_SAKSBLOKK_05.S01_PERSONKEY
                  AND SA_SAK_10.S01_PERSONKEY = SA_HENDELSE_20.S01_PERSONKEY
            """.trimIndent(),
            fnr,
        ) {}
    }
}

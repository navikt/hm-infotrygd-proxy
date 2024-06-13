package no.nav.hjelpemidler.infotrygd.proxy

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.hjelpemidler.infotrygd.proxy.database.JdbcOperations
import java.sql.Connection
import kotlin.test.BeforeTest
import kotlin.test.Test

class InfotrygdDaoTest {
    private val connection: Connection = mockk()
    private val jdbcOperations: JdbcOperations = mockk()
    private val dao: InfotrygdDao = InfotrygdDao(jdbcOperations)

    private val sqlSlot = slot<String>()

    @BeforeTest
    fun setUp() {
        every { connection.prepareStatement(capture(sqlSlot)) } returns mockk()
        every { jdbcOperations.list<Any>(capture(sqlSlot), *anyVararg<Any>(), mapper = any()) } returns emptyList()
    }

    @Test
    fun `Ny og gammel SQL for å hente vedtaksresultat er like`() {
        dao.hentVedtaksresultat("1", "2", "3", "4")
        val nySql = sqlSlot.captured.joinLines()
        sqlSlot.clear()
        getPreparedStatementDecisionResult(connection)
        val eksisterendeSql = sqlSlot.captured
        nySql shouldBe eksisterendeSql
    }

    @Test
    fun `Ny og gammel SQL for å hente antall er like`() {
        dao.antall("1", "2")
        val nySql = sqlSlot.captured.joinLines()
        sqlSlot.clear()
        getPreparedStatementDoesPersonKeyExist(connection)
        val eksisterendeSql = sqlSlot.captured
        nySql shouldBe eksisterendeSql
    }

    @Test
    fun `Ny og gammel SQL for å hente vedtak for er like`() {
        dao.harVedtakFor("1", "2", "3", "4")
        val nySql = sqlSlot.captured.joinLines()
        sqlSlot.clear()
        getPreparedStatementHasDecisionFor(connection)
        val eksisterendeSql = sqlSlot.captured
        nySql shouldBe eksisterendeSql
    }

    @Test
    fun `Ny og gammel SQL for å hente vedtak fra før er like`() {
        dao.harVedtakFraFør("1")
        val nySql = sqlSlot.captured.joinLines()
        sqlSlot.clear()
        getPreparedStatementHarVedtakFraFør(connection)
        val eksisterendeSql = sqlSlot.captured
        nySql shouldBe eksisterendeSql
    }

    @Test
    fun `Ny og gammel SQL for å hente saker er like`() {
        dao.hentSakerForBruker("1")
        val nySql = sqlSlot.captured.joinLines()
        sqlSlot.clear()
        getPreparedStatementHentSakerForBruker(connection)
        val eksisterendeSql = sqlSlot.captured
        nySql shouldBe eksisterendeSql.replace("\\s+".toRegex(), " ")
    }
}

private fun String.joinLines(): String = lines().joinToString(" ", transform = String::trim)

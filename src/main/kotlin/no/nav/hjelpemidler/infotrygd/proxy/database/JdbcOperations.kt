package no.nav.hjelpemidler.infotrygd.proxy.database

import kotliquery.Row
import org.intellij.lang.annotations.Language

typealias RowMapper<T> = (Row) -> T

interface JdbcOperations {
    fun <T : Any> single(
        @Language("Oracle") sql: String,
        queryParameters: QueryParameters = emptyMap(),
        mapper: RowMapper<T>,
    ): T

    fun <T : Any> singleOrNull(
        @Language("Oracle") sql: String,
        queryParameters: QueryParameters = emptyMap(),
        mapper: RowMapper<T>,
    ): T?

    fun <T : Any> list(
        @Language("Oracle") sql: String,
        queryParameters: QueryParameters = emptyMap(),
        mapper: RowMapper<T>,
    ): List<T>

    fun execute(
        @Language("Oracle") sql: String,
        queryParameters: QueryParameters = emptyMap(),
    ): Boolean

    fun batch(
        @Language("Oracle") sql: String,
        queryParameters: Collection<QueryParameters>,
    ): List<Int>

    fun <T : Any> batch(
        @Language("Oracle") sql: String,
        items: Collection<T>,
        block: (T) -> QueryParameters,
    ): List<Int> = batch(sql, items.map(block))
}

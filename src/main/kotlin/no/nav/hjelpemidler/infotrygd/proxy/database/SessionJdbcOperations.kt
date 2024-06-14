package no.nav.hjelpemidler.infotrygd.proxy.database

import kotliquery.Session
import kotliquery.queryOf
import java.io.Closeable

class SessionJdbcOperations(private val session: Session) : JdbcOperations, Closeable by session {
    override fun <T : Any> single(sql: String, queryParameters: QueryParameters, mapper: RowMapper<T>): T {
        return singleOrNull(sql, queryParameters, mapper)
            ?: throw NoSuchElementException("Spørringen ga ingen treff i databasen")
    }

    override fun <T : Any> singleOrNull(sql: String, queryParameters: QueryParameters, mapper: RowMapper<T>): T? {
        return session.single(queryOf(sql, queryParameters.prepare()), mapper)
    }

    override fun <T : Any> list(sql: String, queryParameters: QueryParameters, mapper: RowMapper<T>): List<T> {
        return session.list(queryOf(sql, queryParameters.prepare()), mapper)
    }

    override fun execute(sql: String, queryParameters: QueryParameters): Boolean {
        return session.execute(queryOf(sql, queryParameters.prepare()))
    }

    override fun batch(sql: String, queryParameters: Collection<QueryParameters>): List<Int> {
        return session.batchPreparedNamedStatement(sql, queryParameters.map(QueryParameters::prepare))
    }
}

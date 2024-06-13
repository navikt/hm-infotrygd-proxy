package no.nav.hjelpemidler.infotrygd.proxy.database

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class ConnectionJdbcOperations(private val connection: Connection) : JdbcOperations, AutoCloseable by connection {
    override fun execute(sql: String, vararg parameters: Any): Boolean =
        preparedStatement(sql, parameters.toList()) { statement ->
            statement.execute()
        }

    override fun <T> list(sql: String, vararg parameters: Any, mapper: ResultSet.() -> T): List<T> =
        preparedStatement(sql, parameters.toList()) { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.use {
                    buildList {
                        while (it.next()) {
                            add(mapper(resultSet))
                        }
                    }
                }
            }
        }

    private fun <T> preparedStatement(sql: String, parameters: List<Any>, block: (PreparedStatement) -> T): T =
        connection.prepareStatement(sql).use { statement ->
            parameters.forEachIndexed { index, parameter ->
                val parameterIndex = index + 1
                when (parameter) {
                    is Boolean -> statement.setBoolean(parameterIndex, parameter)
                    is Int -> statement.setInt(parameterIndex, parameter)
                    is Long -> statement.setLong(parameterIndex, parameter)
                    is String -> statement.setString(parameterIndex, parameter)
                    else -> statement.setObject(parameterIndex, parameter)
                }
            }
            block(statement)
        }
}

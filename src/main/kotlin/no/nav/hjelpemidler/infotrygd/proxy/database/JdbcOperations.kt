package no.nav.hjelpemidler.infotrygd.proxy.database

import org.intellij.lang.annotations.Language
import java.sql.ResultSet

interface JdbcOperations {
    fun execute(@Language("Oracle") sql: String, vararg parameters: Any): Boolean
    fun <T> list(@Language("Oracle") sql: String, vararg parameters: Any, mapper: ResultSet.() -> T): List<T>
}

package no.nav.hjelpemidler.infotrygd.proxy.database

import oracle.ucp.jdbc.PoolDataSourceFactory
import javax.sql.DataSource

class DataSourceConfiguration {
    var driverClassName: String = "oracle.jdbc.pool.OracleDataSource"
    var jdbcUrl: String? = null
    var username: String? = null
    var password: String? = null
    var databaseName: String? = null
}

fun createDataSource(block: DataSourceConfiguration.() -> Unit): DataSource {
    val configuration = DataSourceConfiguration().apply(block)
    return PoolDataSourceFactory.getPoolDataSource().apply {
        connectionFactoryClassName = configuration.driverClassName
        url = configuration.jdbcUrl
        user = configuration.username
        password = configuration.password
        databaseName = configuration.databaseName
    }
}

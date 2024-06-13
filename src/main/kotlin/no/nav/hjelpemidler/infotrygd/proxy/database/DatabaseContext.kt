package no.nav.hjelpemidler.infotrygd.proxy.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> withDatabaseContext(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO, block)

package no.nav.hjelpemidler.infotrygd.proxy

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KMarkerFactory

// TODO denne funksjonaliteten finnes i nyere versjoner av Hotlibs. Bruk Hotlibs sin implementering når vi har fått oppdatert til nyere versjon.Add commentMore actions

val teamLogsMarker = KMarkerFactory.getMarker("TEAM_LOGS")
fun KLogger.teamTrace(throwable: Throwable? = null, message: () -> String) = trace(teamLogsMarker, throwable, message)
fun KLogger.teamDebug(throwable: Throwable? = null, message: () -> String) = debug(teamLogsMarker, throwable, message)
fun KLogger.teamInfo(throwable: Throwable? = null, message: () -> String) = info(teamLogsMarker, throwable, message)
fun KLogger.teamWarn(throwable: Throwable? = null, message: () -> String) = warn(teamLogsMarker, throwable, message)
fun KLogger.teamError(throwable: Throwable? = null, message: () -> String) = error(teamLogsMarker, throwable, message)

package no.nav.hjelpemidler.infotrygd.proxy

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.hjelpemidler.configuration.EntraIDEnvironmentVariable
import java.net.URI
import java.util.concurrent.TimeUnit

fun Application.installAuthentication() {
    val jwkProvider = JwkProviderBuilder(URI(EntraIDEnvironmentVariable.AZURE_OPENID_CONFIG_JWKS_URI).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("aad") {
            verifier(jwkProvider, EntraIDEnvironmentVariable.AZURE_OPENID_CONFIG_ISSUER) {
                withAudience(EntraIDEnvironmentVariable.AZURE_APP_CLIENT_ID)
            }
            validate { credentials -> JWTPrincipal(credentials.payload) }
        }
    }
}

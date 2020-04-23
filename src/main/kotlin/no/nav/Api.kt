package no.nav

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import no.nav.security.token.support.ktor.*
import org.slf4j.event.*

@io.ktor.util.KtorExperimentalAPI
fun Application.api(appConfig: ApplicationConfig = this.environment.config) {
   install(CallLogging) {
      level = Level.INFO

      filter { call ->
         !call.request.path().startsWith("/internal")
      }
   }
   install(Authentication) {
      tokenValidationSupport(
         config = appConfig, requiredClaims = RequiredClaims(
         issuer = appConfig.propertyOrNull("no.nav.security.jwt.required_issuer_name")?.getString() ?: "unknown",
         claimMap = arrayOf("acr=Level4")
      )
      )
   }

   install(ContentNegotiation) {
      serialization(contentType = ContentType.Application.Json)
   }

   install(CORS) {
      allowCredentials = true
      method(HttpMethod.Post)
      host("coronapenger-lonnskompensasjon-ui.nais.oera-q.local", listOf("https"))
      host("coronapenger-lonnskompensasjon-ui.nais.oera.no", listOf("https"))
      host("www.nav.no", listOf("https"))
      host("www-q1.nav.no", listOf("https"))
   }

   routing {
      get("/") {
         call.respond("Hello!")
      }

      authenticate {
         get("/protected") {
            call.respond("Hello from protected")
         }
      }
   }
}


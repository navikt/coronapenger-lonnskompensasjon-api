package no.nav

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import no.nav.security.token.support.ktor.*
import org.slf4j.event.*
import java.io.*

@io.ktor.util.KtorExperimentalAPI
val httpClient = HttpClient(CIO) {
   install(JsonFeature) { serializer = KotlinxSerializer() }
}

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
      authenticate {
         get("/protected") {
            call.respond("Hello from protected")
         }
      }

      get("/ping") {
         val url = appConfig.property("no.nav.apigw.base_url").getString()
         try {
            val apiKey = appConfig.property("no.nav.apigw.api_key").getString()
            log.info("Got API key")
            val pingResponse = httpClient.get<HttpResponse>("$url/ping") {
               header("x-nav-apiKey", apiKey)
            }
            call.respond(pingResponse.status, pingResponse.readText())
         } catch (ex: Exception) {
            log.error("Error while calling gateway", ex)
            call.respondText("That didn't go so well...")
         }
      }
   }
}


package no.nav

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.config.ApplicationConfig
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import no.nav.security.token.support.ktor.RequiredClaims
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.event.Level

const val basePath = "/dagpenger/coronapenger-lonnskompensasjon-api"

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

   install(CORS) {
      allowCredentials = true
      method(HttpMethod.Post)
      host("coronapenger-lonnskompensasjon-ui.nais.oera-q.local", listOf("https"))
      host("coronapenger-lonnskompensasjon-ui.nais.oera.no", listOf("https"))
      host("www.nav.no", listOf("https"))
      host("www-q1.nav.no", listOf("https"))
   }

   val apigwBaseUrl = appConfig.property("no.nav.apigw.base_url").getString()
   val httpClient = HttpClient(CIO) {
      install(JsonFeature) { serializer = KotlinxSerializer() }
      defaultRequest {
         header("x-nav-apiKey", appConfig.property("no.nav.apigw.api_key").getString())
      }
   }

   routing {
      authenticate {
         get("/protected") {
            call.respond("Hello from protected")
         }

         get(basePath) {
            val params = call.request.queryParameters
            params["path"]?.let {
               val response = httpClient.get<HttpResponse>("$apigwBaseUrl/${it.removePrefix("/")}") {
                  header(HttpHeaders.Authorization, "Bearer ${jwtFrom(call)}")
                  header(HttpHeaders.Accept, call.request.header(HttpHeaders.Accept))
               }
               call.respondText(
                  text = response.readText(),
                  contentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) } ?: ContentType.Application.Json,
                  status = response.status
               )
            } ?: call.respond(HttpStatusCode.BadRequest, "query parameter path mangler")
         }

         post(basePath) {
            val params = call.request.queryParameters
            params["path"]?.let {
               val response = httpClient.post<HttpResponse>("$apigwBaseUrl/${it.removePrefix("/")}") {
                  header(HttpHeaders.Authorization, "Bearer ${jwtFrom(call)}")
                  header(HttpHeaders.Accept, call.request.header(HttpHeaders.Accept))
                  header(HttpHeaders.ContentType, call.request.header(HttpHeaders.ContentType))
                  body = call.receiveText()
               }
               call.respondText(
                  text = response.readText(),
                  contentType = response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) } ?: ContentType.Application.Json,
                  status = response.status
               )
            } ?: call.respond(HttpStatusCode.BadRequest, "query parameter path mangler")
         }
      }
   }
}

private fun jwtFrom(call: ApplicationCall) = call.request.header("Authorization")?.let {
   it.split(" ")[1]
} ?: call.request.cookies.let {
   it["selvbetjening-idtoken"]
      ?.split(" ")?.get(0)
} ?: throw Exception("Couldn't retrieve jwt from call")


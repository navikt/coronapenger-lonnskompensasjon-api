package no.nav

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import io.ktor.config.MapApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@io.ktor.util.KtorExperimentalAPI
class ApiTest {

   @Ignore
   fun `requests without idtoken are forbidden`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected").apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
         }
      }
   }

   @Ignore
   fun `requests with valid idtoken in bearer header are permitted`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected") {
            addHeader(HttpHeaders.Authorization, "Bearer ${issueToken(acrLevel = "Level4", audience = REQUIRED_AUDIENCE)}")
         }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
         }
      }
   }

   @Ignore
   fun `requests with valid idtoken in cookie are permitted`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).apply {
            put("no.nav.security.jwt.issuers.0.cookie_name", "selvbetjening-idtoken")
         }.setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected") {
            addHeader("Cookie", "selvbetjening-idtoken=${issueToken(acrLevel = "Level4", audience = REQUIRED_AUDIENCE)}")
         }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
         }
      }
   }

   @Ignore
   fun `requests with invalid idtoken are forbidden`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected") {
            addHeader(HttpHeaders.Authorization, "Bearer ${issueToken(acrLevel = "Level4", audience = REQUIRED_AUDIENCE)}bogus")
         }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
         }
      }
   }

   @Ignore
   fun `requests with invalid signature are forbidden`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected") {
            addHeader(HttpHeaders.Authorization, "Bearer $tokenWithInvalidSignature")
         }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
         }
      }
   }

   @Ignore
   fun `requests with wrong acr levels are forbidden`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected") {
            addHeader(HttpHeaders.Authorization, "Bearer ${issueToken(acrLevel = "Level3", audience = REQUIRED_AUDIENCE)}")
         }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
         }
      }
   }

   @Ignore
   fun `requests with wrong audience are forbidden`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "/protected") {
            addHeader(HttpHeaders.Authorization, "Bearer ${issueToken(acrLevel = "Level4", audience = "BogusAudience")}")
         }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
         }
      }
   }

   @Ignore
   fun `should proxy requests to backend`() {
      val token = "Bearer ${issueToken(acrLevel = "Level4", audience = REQUIRED_AUDIENCE)}"

      wireMockServer.stubFor(
         WireMock.get(WireMock.urlPathEqualTo("/api/whatever"))
            .withHeader("x-nav-apiKey", WireMock.equalTo("x-api-key"))
            .withHeader(HttpHeaders.Authorization, WireMock.equalTo(token))
            .withHeader(HttpHeaders.Accept, WireMock.equalTo(ContentType.Application.Json.toString()))
            .willReturn(WireMock.okJson("""{"message":"this is super"}"""))
      )

      wireMockServer.stubFor(
         WireMock.post(WireMock.urlPathEqualTo("/api/whatever"))
            .withHeader("x-nav-apiKey", WireMock.equalTo("x-api-key"))
            .withHeader(HttpHeaders.Authorization, WireMock.equalTo(token))
            .withHeader(HttpHeaders.Accept, WireMock.equalTo(ContentType.Application.Json.toString()))
            .withHeader(HttpHeaders.ContentType, WireMock.equalTo(ContentType.Application.Json.toString()))
            .withRequestBody(AnythingPattern())
            .willReturn(WireMock.okJson("""{"key":"value"}"""))
      )

      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "$basePath?path=/api/whatever") {
            addHeader(HttpHeaders.Authorization, token)
         }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("application/json; charset=UTF-8", response.headers[HttpHeaders.ContentType])
            assertEquals("""{"message":"this is super"}""", response.content)
         }

         handleRequest(HttpMethod.Post, "$basePath?path=/api/whatever") {
            setBody("""{"key":"value"}""")
            addHeader(HttpHeaders.Authorization, token)
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
         }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("application/json; charset=UTF-8", response.headers[HttpHeaders.ContentType])
            assertEquals("""{"key":"value"}""", response.content)
         }
      }
   }

   @Ignore
   fun `should return 400 if query param 'path' is missing`() {
      withTestApplication({
         (environment.config as MapApplicationConfig).setDefaultConfig()
         api()
      }) {
         handleRequest(HttpMethod.Get, "$basePath?no=go") {
            addHeader(HttpHeaders.Authorization, "Bearer ${issueToken(acrLevel = "Level4", audience = REQUIRED_AUDIENCE)}")
         }.apply {
            assertEquals(HttpStatusCode.BadRequest, response.status())
         }
      }
   }

   private fun MapApplicationConfig.setDefaultConfig() {
      put("no.nav.security.jwt.issuers.size", "1")
      put("no.nav.security.jwt.issuers.0.issuer_name", ISSUER_ID)
      put("no.nav.security.jwt.issuers.0.discoveryurl", mockOAuth2Server.wellKnownUrl(ISSUER_ID).toString())
      put("no.nav.security.jwt.issuers.0.accepted_audience", REQUIRED_AUDIENCE)
      put("no.nav.security.jwt.required_issuer_name", ISSUER_ID)
      put("ktor.environment", "local")
      put("no.nav.apigw.base_url", wireMockServer.baseUrl())
      put("no.nav.apigw.api_key", "x-api-key")
   }

   private fun issueToken(acrLevel: String, audience: String): String =
      mockOAuth2Server.issueToken(
         ISSUER_ID,
         "myclient",
         DefaultOAuth2TokenCallback(
            audience = audience,
            claims = mapOf(
               "acr" to acrLevel,
               "sub" to "12345678910"
            )
         )
      ).serialize()

   companion object {
      private const val ISSUER_ID = "da_issuah"
      private const val REQUIRED_AUDIENCE = "default"
      private const val tokenWithInvalidSignature = "eyJraWQiOiJtb2NrLW9hdXRoMi1zZXJ2ZXIta2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJjaGFuZ2VkIiwiYXVkIjoiZGVmYXVsdCIsImFjciI6IkxldmVsNCIsIm5iZiI6MTU4NTA2MjUxMSwiYXpwIjoibXljbGllbnQiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjU0ODMxL2RhX2lzc3VhaCIsImV4cCI6MTU4NTA2NjExMSwiaWF0IjoxNTg1MDYyNTExLCJqdGkiOiIxMTQ3NmViMS01NTc2LTQ1NDItODZmMS03MjM4MGUyNjEwOGIiLCJ0aWQiOiJkZWZhdWx0In0.hoAOkuxtfkFGF6IcGVLY9p-DIqYXFpuePc4zE9e_H7VKnBMQvxlARDi_eeWTEWO4F0RTGyVGOoItmb2uUB-LdLNCmPi8jNSWo8p2Dz-ATrZ3yVYMAOtLlxSCCJWDk0f7HNJ8tMYAFrSrR8dEcE-_SShA4GxuS91uUdLGThJ6Y8hn7dgDIm9bMB1-Rca5H6-BvF_xl4Zbu40QO83sOVUKEEoFYiICH_YSLwdW4nMFaM7XA0xgDCN1IHkG9j72wY_j_-vaejTZ8FiIyv-DB2MirjybJLmHy_xpAJb-7m4itzRHFAdU9vqyn9F6ykTC_SVW1jmlNQZpAq8sXcmo3mL_DA"

      private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      private val mockOAuth2Server = MockOAuth2Server()

      @BeforeAll
      @JvmStatic
      fun setup() {
         wireMockServer.start()
         mockOAuth2Server.start()
      }

      @AfterAll
      @JvmStatic
      fun cleanup() {
         wireMockServer.stop()
         mockOAuth2Server.shutdown()
      }
   }

}

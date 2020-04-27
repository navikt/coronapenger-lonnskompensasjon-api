package no.nav

import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.*
import no.nav.security.mock.oauth2.token.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import kotlin.test.*

@io.ktor.util.KtorExperimentalAPI
class ApiTest {

   @Test
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

   @Test
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

   @Test
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

   @Test
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

   @Test
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

   @Test
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

   @Test
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

   private fun MapApplicationConfig.setDefaultConfig() {
      put("no.nav.security.jwt.issuers.size", "1")
      put("no.nav.security.jwt.issuers.0.issuer_name", ISSUER_ID)
      put("no.nav.security.jwt.issuers.0.discoveryurl", mockOAuth2Server.wellKnownUrl(ISSUER_ID).toString())
      put("no.nav.security.jwt.issuers.0.accepted_audience", REQUIRED_AUDIENCE)
      put("no.nav.security.jwt.required_issuer_name", ISSUER_ID)
      put("ktor.environment", "local")
      put("no.nav.apigw.base_url", "http://localhost")
      put("no.nav.apigw.api_key", "http://localhost")
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

      val mockOAuth2Server = MockOAuth2Server()

      @BeforeAll
      @JvmStatic
      fun setup() {
         mockOAuth2Server.start()
      }

      @AfterAll
      @JvmStatic
      fun cleanup() {
         mockOAuth2Server.shutdown()
      }
   }

}

package no.nav

import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class NaisTest {

   @Test
   fun `isalive ping from nais`() {
      withTestApplication({
         nais()
      }) {
         handleRequest(HttpMethod.Get, "/internal/isready").apply {
            assertTrue { response.status()?.isSuccess() ?: false }
         }
      }
   }

   @Test
   fun `isready ping from nais`() {
      withTestApplication({
         nais()
      }) {
         handleRequest(HttpMethod.Get, "/internal/isready").apply {
            assertTrue { response.status()?.isSuccess() ?: false }
         }
      }
   }

   @Test
   fun `reports metrics`() {
      withTestApplication({
         nais()
      }) {
         handleRequest(HttpMethod.Get, "/internal/metrics").apply {
            assertTrue { response.status()?.isSuccess() ?: false }
         }
      }
   }
}

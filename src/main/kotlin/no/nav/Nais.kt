package no.nav

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.*
import io.micrometer.prometheus.*
import io.prometheus.client.*
import io.prometheus.client.exporter.common.*

fun Application.nais() {
   install(MicrometerMetrics) {
      registry = PrometheusMeterRegistry(
         PrometheusConfig.DEFAULT,
         CollectorRegistry.defaultRegistry,
         Clock.SYSTEM
      )
      meterBinders = listOf(
         ClassLoaderMetrics(),
         JvmMemoryMetrics(),
         JvmGcMetrics(),
         ProcessorMetrics(),
         JvmThreadMetrics()
      )
   }

   routing {
      get("/internal/isalive") {
         call.respond(OK)
      }

      get("/internal/isready") {
         call.respond(OK)
      }

      get("/internal/metrics") {
         val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
         call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
         }
      }
   }
}

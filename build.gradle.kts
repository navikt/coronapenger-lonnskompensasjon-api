import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.*

val junitJupiterVersion = "5.6.2"
val ktorVersion = "1.3.1"
val micrometerVersion = "1.3.5"
val slf4jVersion = "1.7.30"
val log4jVersion = "2.16.0"
val logstashEncoderVersion = "6.3"
val coroutinesVersion = "1.3.3"
val serializerVersion = "0.14.0"
val tokenValidatorVersion = "1.1.4"
val mockOauthVersion = "0.1.31"
val s3sdkVersion = "1.11.804"
val localstackVersion = "1.14.3"
val jaxbVersion = "2.3.1"

group = "no.nav.corona"

plugins {
   kotlin("jvm") version "1.3.71"
   kotlin("plugin.serialization") version "1.3.72"
}

repositories {
   mavenCentral()
}

dependencies {
   implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
   implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializerVersion")
   implementation("io.ktor:ktor-client-cio:$ktorVersion")
   implementation("io.ktor:ktor-server-netty:$ktorVersion")
   implementation("io.ktor:ktor-auth:$ktorVersion")
   implementation("io.ktor:ktor-serialization:$ktorVersion")
   implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
   implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
   implementation("io.ktor:ktor-client-json:$ktorVersion")

   implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

   implementation("org.slf4j:slf4j-api:$slf4jVersion")
   implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
   implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
   implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
   implementation("com.vlkan.log4j2:log4j2-logstash-layout-fatjar:0.19")

   implementation("no.nav.security:token-validation-ktor:$tokenValidatorVersion")

   implementation("com.amazonaws:aws-java-sdk-s3:$s3sdkVersion")
   implementation("javax.xml.bind:jaxb-api:$jaxbVersion")

   testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
   testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

   testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
      exclude(group = "junit")
   }
   testImplementation("org.assertj:assertj-core:3.15.0")
   testImplementation("no.nav.security:mock-oauth2-server:$mockOauthVersion")
   testImplementation("com.github.tomakehurst:wiremock-standalone:2.23.2")

   testImplementation("org.testcontainers:localstack:$localstackVersion")
}

java {
   sourceCompatibility = JavaVersion.VERSION_12
   targetCompatibility = JavaVersion.VERSION_12
}

tasks.withType<KotlinCompile> {
   kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Jar>("jar") {
   archiveBaseName.set("app")

   manifest {
      attributes["Main-Class"] = "no.nav.AppKt"
      attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
         it.name
      }
   }

   doLast {
      configurations.runtimeClasspath.get().forEach {
         val file = File("$buildDir/libs/${it.name}")
         if (!file.exists())
            it.copyTo(file)
      }
   }
}

tasks.withType<Test> {
   useJUnitPlatform()
   testLogging {
      events("passed", "skipped", "failed")
      exceptionFormat = FULL
   }
}

tasks.withType<Wrapper> {
   gradleVersion = "6.3"
}

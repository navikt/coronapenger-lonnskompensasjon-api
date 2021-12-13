package no.nav.mellomlagring

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CreateBucketRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.localstack.LocalStackContainer

@TestInstance(Lifecycle.PER_CLASS)
class BlockStorageTest {
   private val bucketName = "tullebucket"
   private lateinit var s3Container: LocalStackContainer
   private lateinit var s3: AmazonS3

   @Ignore
   fun `store file and read it back`() {
      val blockStorage = BlockStorage(s3, bucketName)
      val fileContents = "yolo".toByteArray()
      blockStorage.writeFile("tullefil", fileContents, "themimetype")
      val expected = BlockStorageFile("tullefil", fileContents)
      val actual = blockStorage.readFile("tullefil")
      assertEquals(expected, actual)
   }

   @BeforeAll
   fun setup() {
      s3Container = LocalStackContainer().withServices(LocalStackContainer.Service.S3).apply {
         start()
      }
      s3 = AmazonS3ClientBuilder
         .standard()
         .withEndpointConfiguration(s3Container.getEndpointConfiguration(LocalStackContainer.Service.S3))
         .withCredentials(s3Container.defaultCredentialsProvider)
         .withChunkedEncodingDisabled(true)
         .withPathStyleAccessEnabled(true)
         .build()
         .apply {
            createBucket(CreateBucketRequest(bucketName).withCannedAcl(CannedAccessControlList.PublicRead))
         }
   }

   @AfterAll
   fun teardown() {
      s3Container.stop()
   }

}

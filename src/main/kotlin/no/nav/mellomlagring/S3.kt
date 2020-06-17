package no.nav.mellomlagring

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CreateBucketRequest

fun s3From(accessKey: String, secretKey: String, url: String, region: String, bucket: String): AmazonS3 {
   val credentials: AWSCredentials = BasicAWSCredentials(accessKey, secretKey)
   val client = AmazonS3ClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(url, region))
      .enablePathStyleAccess()
      .withCredentials(AWSStaticCredentialsProvider(credentials)).build()
   ensureBucketExists(client, bucket)
   return client
}

private fun ensureBucketExists(s3: AmazonS3, bucket: String) {
   val bucketExists: Boolean = s3.listBuckets().any { b: Bucket -> b.name == bucket }
   if (!bucketExists) {
      s3.createBucket(CreateBucketRequest(bucket)
         .withCannedAcl(CannedAccessControlList.Private))
   }
}

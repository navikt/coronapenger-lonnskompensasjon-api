package no.nav.mellomlagring

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import kotlinx.io.ByteArrayInputStream

class BlockStorage(private val s3: AmazonS3, private val bucketName: String) {
   val filenamePrefix = "mellomlagring/"

   fun writeFile(name: String, contents: ByteArray, mimeType: String): String {
      val metadata = ObjectMetadata().apply {
         contentType = mimeType
         contentLength = contents.size.toLong()
         addUserMetadata("origname", name)
      }
      return s3.putObject(bucketName, "$filenamePrefix-$name", ByteArrayInputStream(contents), metadata).contentMd5
   }

   fun readFile(name: String) =
      s3.getObject(GetObjectRequest(bucketName, "$filenamePrefix-$name")).let {
         val contents = it.objectContent.use {
            it.readAllBytes()
         }
         val title = it.objectMetadata.userMetadata["origname"]?.toString() ?: "unknown_filename"
         BlockStorageFile(title, contents)
      }


}

class BlockStorageFile(val name: String, val contents: ByteArray) {
   override fun equals(other: Any?): Boolean {
      return other is BlockStorageFile && other.name == this.name && other.contents.contentEquals(this.contents)
   }
}

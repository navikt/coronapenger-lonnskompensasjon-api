package no.nav.mellomlagring

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class Krypto(passphrase: String, fnr: String) {
   private val key: SecretKey
   private val iv: String

   fun encrypt(plainText: ByteArray) = cipher(Cipher.ENCRYPT_MODE).doFinal(plainText)

   fun decrypt(encrypted: ByteArray) = cipher(Cipher.DECRYPT_MODE).doFinal(encrypted)

   private fun cipher(mode: Int): Cipher {
      val cipher = Cipher.getInstance(ALGO)
      cipher.init(mode, key, GCMParameterSpec(128, iv.toByteArray()))
      return cipher
   }

   private fun key(passphrase: String, salt: String) =
      SecretKeySpec(
         SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt.toByteArray(), 10000, 256)).encoded,
         "AES")

   private fun isEmpty(str: String) = str.trim().isEmpty()

   companion object {
      private const val ALGO = "AES/GCM/NoPadding"
   }

   init {
      require(!(isEmpty(passphrase) || isEmpty(fnr))) { "Both passphrase and fnr must be provided" }
      key = key(passphrase, fnr)
      iv = fnr
   }

}

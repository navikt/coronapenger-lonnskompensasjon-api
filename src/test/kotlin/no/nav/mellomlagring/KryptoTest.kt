package no.nav.mellomlagring

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KryptoTest {

   @Test
   fun `plaintext and ciphertext must not be the same`() {
      val plaintext = "yolo".toByteArray()
      val krypto = Krypto("el passo phraso", "12345678910")
      val ciphertext = krypto.encrypt(plaintext)
      assertFalse(plaintext.contentEquals(ciphertext))
   }

   @Test
   fun `encrypt and then decrypt with same params yields same plaintext`() {
      val plaintext = "yolo".toByteArray()
      val krypto = Krypto("el passo phraso", "12345678910")
      val ciphertext = krypto.encrypt(plaintext)
      val decrypted = krypto.decrypt(ciphertext)
      assertTrue(plaintext.contentEquals(decrypted))
   }

   @Test
   fun `different fnr generates different ciphertext`() {
      val plaintext = "yolo".toByteArray()
      val krypto1 = Krypto("el passo phraso", "12345678910")
      val krypto2 = Krypto("el passo phraso", "12345678911")
      val ciphertext1 = krypto1.encrypt(plaintext)
      val ciphertext2 = krypto2.encrypt(plaintext)
      assertFalse(ciphertext1.contentEquals(ciphertext2))
   }

   @Test
   fun `different passphrase generates different ciphertext`() {
      val plaintext = "yolo".toByteArray()
      val krypto1 = Krypto("el passo phraso", "12345678910")
      val krypto2 = Krypto("el phraso passo", "12345678910")
      val ciphertext1 = krypto1.encrypt(plaintext)
      val ciphertext2 = krypto2.encrypt(plaintext)
      assertFalse(ciphertext1.contentEquals(ciphertext2))
   }

}

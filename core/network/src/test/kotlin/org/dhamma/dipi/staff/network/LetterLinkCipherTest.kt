package org.dhamma.dipi.staff.network

import org.junit.Assert.*
import org.junit.Test

class LetterLinkCipherTest {
    // Generated independently with PHP openssl_encrypt using synthetic secrets only.
    private val key = "dipi-synthetic-test-key".toByteArray()
    private val iv = "dipi-synthetic-test-iv".toByteArray()

    @Test fun `matches PHP double Base64 and hexadecimal key derivation`() {
        assertEquals("M0FvSytEd0tzcXRCK2FlWmZUQS8vZz09", LetterLinkCipher.encrypt(12345, 987, key, iv))
        assertEquals("Z1NlSjAwRTgzaXNKTEY5d1liYzhxQT09", LetterLinkCipher.encrypt(1, 1, key, iv))
        assertEquals("MlZpbjlVWWsrbmtlTmxIb3pTQ1NWRHVRWitJcEE3UTdiOTF1TVVRN3J5VT0=", LetterLinkCipher.encrypt(123456789, 123456, key, iv))
    }

    @Test fun `does not mutate caller secrets and changes token for each identity`() {
        val keyBefore = key.copyOf()
        val ivBefore = iv.copyOf()
        val first = LetterLinkCipher.encrypt(12345, 987, key, iv)
        assertNotEquals(first, LetterLinkCipher.encrypt(12346, 987, key, iv))
        assertNotEquals(first, LetterLinkCipher.encrypt(12345, 988, key, iv))
        assertArrayEquals(keyBefore, key)
        assertArrayEquals(ivBefore, iv)
    }

    @Test fun `rejects missing provisioning and invalid identifiers`() {
        listOf(0 to 1, 1 to 0, -1 to 1).forEach { (applicant, letter) ->
            assertThrows(IllegalArgumentException::class.java) { LetterLinkCipher.encrypt(applicant, letter, key, iv) }
        }
        assertThrows(IllegalArgumentException::class.java) { LetterLinkCipher.encrypt(1, 1, byteArrayOf(), iv) }
        assertThrows(IllegalArgumentException::class.java) { LetterLinkCipher.encrypt(1, 1, key, byteArrayOf()) }
    }
}

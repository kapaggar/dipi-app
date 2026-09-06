package org.dhamma.dipi.staff.network

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Compatibility with Drupal simple_crypt, NOT a new encryption protocol.
 * PHP hashes to HEX TEXT, OpenSSL truncates that text to 32 bytes, and its
 * default Base64 output is Base64-encoded again. A raw SHA-256 key is wrong.
 * Production secrets are supplied by the caller, never constants in the APK.
 */
object LetterLinkCipher {
    fun encrypt(applicantId: Int, letterId: Int, secretKey: ByteArray, secretIv: ByteArray): String {
        require(applicantId > 0 && letterId > 0) { "Invalid letter identifiers" }
        require(secretKey.isNotEmpty() && secretIv.isNotEmpty()) { "Letter key is not configured" }
        val key = sha256Hex(secretKey).copyOf(32)
        val iv = sha256Hex(secretIv).copyOf(16)
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val ciphertext = cipher.doFinal("$applicantId-$letterId".toByteArray(Charsets.US_ASCII))
            Base64.getEncoder().encodeToString(Base64.getEncoder().encode(ciphertext))
        } finally {
            key.fill(0)
            iv.fill(0)
        }
    }

    /** Uses the effective material decoded from the separately provisioned single code. */
    fun encryptMaterial(applicantId: Int, letterId: Int, key: ByteArray, iv: ByteArray): String {
        require(applicantId > 0 && letterId > 0 && key.size == 32 && iv.size == 16) { "Invalid letter encryption material" }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val ciphertext = cipher.doFinal("$applicantId-$letterId".toByteArray(Charsets.US_ASCII))
        return Base64.getEncoder().encodeToString(Base64.getEncoder().encode(ciphertext))
    }

    private fun sha256Hex(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .toByteArray(Charsets.US_ASCII)
}

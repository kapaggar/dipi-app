package org.dhamma.dipi.staff.model

import java.security.MessageDigest
import java.util.Base64

/** One separately distributed bearer secret containing the existing effective AES key and IV. */
object WhatsAppProvisioningCode {
    private const val PREFIX = "DIPI-WA1."
    fun fromLegacySecrets(secret: ByteArray, secretIv: ByteArray): String {
        require(secret.isNotEmpty() && secretIv.isNotEmpty()) { "Enter both existing encryption values" }
        val material = (hexHash(secret).take(32) + hexHash(secretIv).take(16)).toByteArray(Charsets.US_ASCII)
        return try { PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(material) + "." + hexHash(material).take(8) }
        finally { material.fill(0) }
    }
    fun <T> withMaterial(input: String, block: (ByteArray, ByteArray) -> T): T {
        val code = input.trim()
        require(code.matches(Regex("DIPI-WA1\\.[A-Za-z0-9_-]{64}\\.[0-9a-f]{8}"))) { "Enter a complete DIPI WhatsApp provisioning code" }
        val parts = code.removePrefix(PREFIX).split('.')
        val material = Base64.getUrlDecoder().decode(parts[0])
        var key: ByteArray? = null
        var iv: ByteArray? = null
        try {
            require(material.size == 48 && material.all { it.toInt().toChar() in "0123456789abcdef" } && hexHash(material).take(8) == parts[1]) { "Provisioning code is invalid or was copied incorrectly" }
            key = material.copyOfRange(0, 32)
            iv = material.copyOfRange(32, 48)
            return block(key, iv)
        } finally { material.fill(0); key?.fill(0); iv?.fill(0) }
    }
    private fun hexHash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

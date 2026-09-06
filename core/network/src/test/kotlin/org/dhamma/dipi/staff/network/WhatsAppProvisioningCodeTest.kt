package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.WhatsAppProvisioningCode
import org.junit.Assert.*
import org.junit.Test

class WhatsAppProvisioningCodeTest {
    @Test fun `single constant code produces exactly the existing PHP compatible ciphertext`() {
        val key = "synthetic-key".toByteArray(); val iv = "different-synthetic-IV".toByteArray()
        val code = WhatsAppProvisioningCode.fromLegacySecrets(key, iv)
        assertEquals(code, WhatsAppProvisioningCode.fromLegacySecrets(key, iv))
        var held: ByteArray? = null
        val actual = WhatsAppProvisioningCode.withMaterial(code) { k, v -> held = k; LetterLinkCipher.encryptMaterial(123, 456, k, v) }
        assertEquals(LetterLinkCipher.encrypt(123, 456, key, iv), actual)
        assertTrue(held!!.all { it == 0.toByte() })
    }
    @Test fun `rejects truncated mistyped and unsupported provisioning codes`() {
        val code = WhatsAppProvisioningCode.fromLegacySecrets("synthetic-key".toByteArray(), "synthetic-iv".toByteArray())
        for (bad in listOf("", code.dropLast(1), code.replace("WA1", "WA2"), code.dropLast(1) + if (code.last() == '0') "1" else "0")) {
            try { WhatsAppProvisioningCode.withMaterial(bad) { _, _ -> fail("Invalid code reached cipher") }; fail("Accepted invalid code") }
            catch (_: IllegalArgumentException) { }
        }
    }
}

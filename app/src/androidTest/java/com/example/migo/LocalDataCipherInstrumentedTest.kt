package com.example.migo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.migo.data.local.LocalDataCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDataCipherInstrumentedTest {
    private val cipher = LocalDataCipher(keyAlias = "migo_cipher_test_key")

    @Test
    fun encryptThenDecrypt_returnsOriginalTurkishText() {
        val plain = "İstanbul Teknik Üniversitesi — ığşçöü"

        val stored = cipher.encrypt(plain)

        assertTrue(cipher.isEncrypted(stored))
        assertFalse(stored.contains("İstanbul"))
        assertEquals(plain, cipher.decrypt(stored))
    }

    @Test
    fun encrypt_usesFreshIvEachTime() {
        assertNotEquals(cipher.encrypt("aynı metin"), cipher.encrypt("aynı metin"))
    }

    @Test
    fun decrypt_passesLegacyPlainTextThrough() {
        assertEquals("{\"legacy\":true}", cipher.decrypt("{\"legacy\":true}"))
    }

    @Test
    fun decrypt_rejectsTamperedCiphertext() {
        val stored = cipher.encrypt("gizli")
        val tampered = stored.dropLast(2) + if (stored.endsWith("AA")) "BB" else "AA"

        assertThrows(Exception::class.java) { cipher.decrypt(tampered) }
    }
}

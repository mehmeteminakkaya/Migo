package com.example.migo.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class LocalDataCipher(
    private val keyAlias: String = KEY_ALIAS
) {
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = Base64.encodeToString(cipher.iv, BASE64_FLAGS)
        val encrypted = Base64.encodeToString(
            cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)),
            BASE64_FLAGS
        )
        return "$PREFIX$iv:$encrypted"
    }

    fun decrypt(storedValue: String): String {
        if (!isEncrypted(storedValue)) return storedValue

        val parts = storedValue.removePrefix(PREFIX).split(':', limit = 2)
        require(parts.size == 2) { "Geçersiz şifreli veri biçimi." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(parts[0], BASE64_FLAGS))
        )
        return cipher.doFinal(Base64.decode(parts[1], BASE64_FLAGS)).toString(Charsets.UTF_8)
    }

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "migo_local_data_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val PREFIX = "migo:v1:"
        const val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING
    }
}

package com.lifedbg.mobile.llm.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences("life_dbg_api_keys", Context.MODE_PRIVATE)

    fun saveApiKey(providerId: String, apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("${providerId}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${providerId}_value", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun getApiKey(providerId: String): String? {
        val iv = preferences.getString("${providerId}_iv", null) ?: return null
        val value = preferences.getString("${providerId}_value", null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
        )
        val plain = cipher.doFinal(Base64.decode(value, Base64.NO_WRAP))
        return plain.toString(Charsets.UTF_8)
    }

    fun deleteApiKey(providerId: String) {
        preferences.edit()
            .remove("${providerId}_iv")
            .remove("${providerId}_value")
            .apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "life_debugger_llm_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

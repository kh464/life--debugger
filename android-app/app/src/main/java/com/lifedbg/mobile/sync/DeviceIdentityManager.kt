package com.lifedbg.mobile.sync

import android.os.Build
import android.util.Base64
import com.lifedbg.mobile.db.dao.DeviceIdentityDao
import com.lifedbg.mobile.db.entity.DeviceIdentityEntity
import java.security.SecureRandom
import java.util.UUID

class DeviceIdentityManager(
    private val dao: DeviceIdentityDao,
) {
    suspend fun getOrCreateIdentity(): DeviceIdentityEntity {
        dao.getIdentity()?.let { return it }

        val now = System.currentTimeMillis()
        val identity = DeviceIdentityEntity(
            deviceId = UUID.randomUUID().toString(),
            deviceName = defaultDeviceName(),
            deviceType = "android",
            publicKey = randomBase64(32),
            privateKeyRef = null,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(identity)
        return identity
    }

    suspend fun updateName(deviceName: String): DeviceIdentityEntity {
        val identity = getOrCreateIdentity()
        val cleaned = deviceName.trim()
        require(cleaned.isNotEmpty()) { "Device name cannot be empty." }
        dao.updateName(identity.deviceId, cleaned, System.currentTimeMillis())
        return getOrCreateIdentity()
    }

    suspend fun resetIdentity() {
        dao.clear()
    }

    private fun defaultDeviceName(): String {
        val model = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"
        val manufacturer = Build.MANUFACTURER?.takeIf { it.isNotBlank() }.orEmpty()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Life Debugger Android" }
    }

    private fun randomBase64(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
    }
}

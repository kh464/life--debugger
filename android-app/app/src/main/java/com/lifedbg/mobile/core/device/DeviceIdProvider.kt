package com.lifedbg.mobile.core.device

import android.content.Context
import java.util.UUID

class DeviceIdProvider(context: Context) {
    private val preferences = context.getSharedPreferences("life_dbg_device", Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        val existing = preferences.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing

        val created = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}

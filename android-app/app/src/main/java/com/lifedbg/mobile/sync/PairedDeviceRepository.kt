package com.lifedbg.mobile.sync

import com.lifedbg.mobile.db.dao.PairedDeviceDao
import com.lifedbg.mobile.db.entity.PairedDeviceEntity

class PairedDeviceRepository(
    private val dao: PairedDeviceDao,
) {
    suspend fun getAll(): List<PairedDeviceEntity> = dao.getAll()

    suspend fun upsert(device: PairedDeviceEntity) {
        dao.upsert(device)
    }

    suspend fun delete(deviceId: String) {
        dao.delete(deviceId)
    }

    suspend fun isPaired(deviceId: String): Boolean = dao.isPaired(deviceId)
}

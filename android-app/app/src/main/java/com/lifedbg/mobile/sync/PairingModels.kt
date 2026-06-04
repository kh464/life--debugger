package com.lifedbg.mobile.sync

import com.lifedbg.mobile.db.entity.DeviceIdentityEntity
import com.lifedbg.mobile.db.entity.PairedDeviceEntity

const val PAIRING_PROTOCOL = "lifedbg-pairing-v1"

data class PairingQrPayload(
    val protocol: String,
    val sessionId: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val host: String,
    val port: Int,
    val publicKey: String,
    val pairingToken: String,
    val expiresAt: Long,
)

data class PairResponse(
    val status: String,
    val protocol: String?,
    val sessionId: String?,
    val deviceId: String?,
    val deviceName: String?,
    val deviceType: String?,
    val publicKey: String?,
    val pairedAt: Long?,
    val message: String,
    val errorCode: String?,
)

data class DeviceSyncState(
    val localIdentity: DeviceIdentityEntity?,
    val pairedDevices: List<PairedDeviceEntity>,
    val status: String,
    val parsedPayload: PairingQrPayload?,
    val busy: Boolean,
)

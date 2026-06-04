package com.lifedbg.mobile.sync

import com.lifedbg.mobile.db.entity.PairedDeviceEntity

class PairingRepository(
    private val identityManager: DeviceIdentityManager,
    private val pairingClient: PairingClient,
    private val pairedDeviceRepository: PairedDeviceRepository,
    private val parser: PairingQrParser = PairingQrParser(),
) {
    fun parseQrPayload(rawQrText: String): Result<PairingQrPayload> = parser.parse(rawQrText)

    suspend fun pairByPayload(payload: PairingQrPayload): Result<PairedDeviceEntity> {
        if (pairedDeviceRepository.isPaired(payload.deviceId)) {
            return Result.failure(IllegalStateException("This desktop is already paired. Unpair it first if you want to pair again."))
        }

        val identity = identityManager.getOrCreateIdentity()
        return pairingClient.pairWithDesktop(payload, identity).mapCatching { response ->
            require(response.protocol == PAIRING_PROTOCOL) { "Desktop returned an unsupported protocol." }
            require(response.sessionId == payload.sessionId) { "Desktop returned a different pairing session." }
            require(response.deviceId == payload.deviceId) { "Desktop identity does not match the QR payload." }
            require(response.deviceType == "desktop") { "Paired device is not a desktop." }
            require(!response.publicKey.isNullOrBlank()) { "Desktop public key is missing." }

            val pairedAt = response.pairedAt ?: System.currentTimeMillis()
            val device = PairedDeviceEntity(
                pairedDeviceId = response.deviceId,
                pairedDeviceName = response.deviceName.orEmpty().ifBlank { payload.deviceName },
                pairedDeviceType = response.deviceType,
                publicKey = response.publicKey,
                lastKnownHost = payload.host,
                lastKnownPort = payload.port,
                pairingStatus = "paired",
                trustLevel = "trusted",
                pairedAt = pairedAt,
                lastSeenAt = pairedAt,
                lastSyncAt = null,
                metadataJson = null,
            )
            pairedDeviceRepository.upsert(device)
            device
        }
    }
}

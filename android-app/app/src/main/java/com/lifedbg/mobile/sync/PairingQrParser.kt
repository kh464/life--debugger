package com.lifedbg.mobile.sync

import org.json.JSONObject

class PairingQrParser {
    fun parse(rawText: String): Result<PairingQrPayload> = runCatching {
        val root = JSONObject(rawText.trim())
        val payload = PairingQrPayload(
            protocol = root.getString("protocol"),
            sessionId = root.getString("sessionId"),
            deviceId = root.getString("deviceId"),
            deviceName = root.getString("deviceName"),
            deviceType = root.getString("deviceType"),
            host = root.getString("host"),
            port = root.getInt("port"),
            publicKey = root.getString("publicKey"),
            pairingToken = root.getString("pairingToken"),
            expiresAt = root.getLong("expiresAt"),
        )

        require(payload.protocol == PAIRING_PROTOCOL) { "Unsupported pairing protocol." }
        require(payload.deviceType == "desktop") { "Only desktop pairing payloads are supported." }
        require(payload.host.isNotBlank() && payload.port > 0) { "Desktop network address is invalid." }
        require(payload.publicKey.isNotBlank()) { "Desktop public key is missing." }
        require(payload.expiresAt > System.currentTimeMillis()) { "Pairing QR code has expired." }
        payload
    }
}

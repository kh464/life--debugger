package com.lifedbg.mobile.sync

import com.lifedbg.mobile.db.entity.DeviceIdentityEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PairingClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun pairWithDesktop(
        qrPayload: PairingQrPayload,
        localIdentity: DeviceIdentityEntity,
    ): Result<PairResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyJson = JSONObject()
                .put("protocol", PAIRING_PROTOCOL)
                .put("sessionId", qrPayload.sessionId)
                .put("pairingToken", qrPayload.pairingToken)
                .put("deviceId", localIdentity.deviceId)
                .put("deviceName", localIdentity.deviceName)
                .put("deviceType", localIdentity.deviceType)
                .put("publicKey", localIdentity.publicKey)
                .put("timestamp", System.currentTimeMillis())

            val request = Request.Builder()
                .url("http://${qrPayload.host}:${qrPayload.port}/api/pair")
                .header("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val root = JSONObject(responseBody)
                val pairResponse = PairResponse(
                    status = root.getString("status"),
                    protocol = root.optString("protocol").takeIf { it.isNotBlank() },
                    sessionId = root.optString("sessionId").takeIf { it.isNotBlank() },
                    deviceId = root.optString("deviceId").takeIf { it.isNotBlank() },
                    deviceName = root.optString("deviceName").takeIf { it.isNotBlank() },
                    deviceType = root.optString("deviceType").takeIf { it.isNotBlank() },
                    publicKey = root.optString("publicKey").takeIf { it.isNotBlank() },
                    pairedAt = root.optLong("pairedAt").takeIf { it > 0L },
                    message = root.optString("message"),
                    errorCode = root.optString("errorCode").takeIf { it.isNotBlank() },
                )
                if (!response.isSuccessful || pairResponse.status != "ok") {
                    throw IOException(pairResponse.errorCode ?: pairResponse.message.ifBlank { "Pairing failed." })
                }
                pairResponse
            }
        }
    }
}

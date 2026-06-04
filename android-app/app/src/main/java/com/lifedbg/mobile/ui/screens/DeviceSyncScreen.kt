package com.lifedbg.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lifedbg.mobile.LifeDbgApp
import com.lifedbg.mobile.db.entity.DeviceIdentityEntity
import com.lifedbg.mobile.db.entity.PairedDeviceEntity
import com.lifedbg.mobile.sync.DeviceIdentityManager
import com.lifedbg.mobile.sync.PairedDeviceRepository
import com.lifedbg.mobile.sync.PairingClient
import com.lifedbg.mobile.sync.PairingQrPayload
import com.lifedbg.mobile.sync.PairingRepository
import kotlinx.coroutines.launch

@Composable
fun DeviceSyncScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as LifeDbgApp
    val database = app.database
    val identityManager = remember { DeviceIdentityManager(database.deviceIdentityDao()) }
    val pairedDeviceRepository = remember { PairedDeviceRepository(database.pairedDeviceDao()) }
    val pairingRepository = remember {
        PairingRepository(
            identityManager = identityManager,
            pairingClient = PairingClient(),
            pairedDeviceRepository = pairedDeviceRepository,
        )
    }
    val scope = rememberCoroutineScope()

    var identity by remember { mutableStateOf<DeviceIdentityEntity?>(null) }
    var deviceName by remember { mutableStateOf("") }
    var pairedDevices by remember { mutableStateOf<List<PairedDeviceEntity>>(emptyList()) }
    var rawPayload by remember { mutableStateOf("") }
    var parsedPayload by remember { mutableStateOf<PairingQrPayload?>(null) }
    var status by remember { mutableStateOf("准备就绪。") }
    var busy by remember { mutableStateOf(false) }
    var scannerOpen by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        scannerOpen = granted
        status = if (granted) "相机已打开，请扫描电脑端二维码。" else "未获得相机权限，可继续粘贴二维码内容完成配对。"
    }

    fun refresh() {
        scope.launch {
            identity = identityManager.getOrCreateIdentity().also { deviceName = it.deviceName }
            pairedDevices = pairedDeviceRepository.getAll()
        }
    }

    fun parsePayload(rawText: String) {
        rawPayload = rawText
        pairingRepository.parseQrPayload(rawText)
            .onSuccess {
                parsedPayload = it
                scannerOpen = false
                status = "已读取电脑：${it.deviceName}（${it.host}:${it.port}），请确认这是你的电脑。"
            }
            .onFailure {
                parsedPayload = null
                status = it.message ?: "二维码内容无效。"
            }
    }

    LaunchedEffect(Unit) {
        identity = identityManager.getOrCreateIdentity().also { deviceName = it.deviceName }
        pairedDevices = pairedDeviceRepository.getAll()
    }

    ScreenScaffold(
        title = "设备与同步",
        subtitle = "第三阶段只完成手机与电脑的首次可信配对，暂不同步任何行为摘要数据。",
    ) {
        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("本机 Android 身份")
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("设备名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("类型：${identity?.deviceType ?: "android"}")
                Text("Device ID：${identity?.deviceId ?: "loading"}")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                status = runCatching {
                                    identity = identityManager.updateName(deviceName).also { deviceName = it.deviceName }
                                    "设备名称已保存。"
                                }.getOrElse { it.message ?: "保存失败。" }
                                busy = false
                            }
                        },
                    ) {
                        Text("保存名称")
                    }
                    OutlinedButton(enabled = !busy, onClick = { refresh() }) {
                        Text("刷新")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("连接电脑")
                Text("推荐直接扫描电脑端 Devices 页面二维码。模拟器或相机不可用时，也可以粘贴二维码内容 JSON。")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (cameraGranted) {
                                scannerOpen = !scannerOpen
                                status = if (scannerOpen) "相机已打开，请扫描电脑端二维码。" else "扫码已关闭。"
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    ) {
                        Text(if (scannerOpen) "关闭扫码" else "扫描电脑二维码")
                    }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            rawPayload = ""
                            parsedPayload = null
                            scannerOpen = false
                            status = "已清空二维码内容。"
                        },
                    ) {
                        Text("清空")
                    }
                }

                if (scannerOpen) {
                    PairingQrScannerView(onQrText = ::parsePayload)
                }

                OutlinedTextField(
                    value = rawPayload,
                    onValueChange = { rawPayload = it },
                    label = { Text("二维码内容 JSON（调试备用）") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !busy && rawPayload.isNotBlank(),
                    onClick = { parsePayload(rawPayload) },
                ) {
                    Text("读取二维码内容")
                }

                parsedPayload?.let { payload ->
                    InfoCard(
                        title = "即将连接电脑",
                        body = "设备名：${payload.deviceName}\n类型：${payload.deviceType}\n局域网地址：${payload.host}:${payload.port}\n请确认这是你自己的电脑。",
                    )
                    Button(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                status = pairingRepository.pairByPayload(payload)
                                    .map {
                                        parsedPayload = null
                                        rawPayload = ""
                                        pairedDevices = pairedDeviceRepository.getAll()
                                        "配对成功：${it.pairedDeviceName}"
                                    }
                                    .getOrElse { pairingErrorText(it) }
                                busy = false
                            }
                        },
                    ) {
                        Text("确认配对")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("已配对设备")
                if (pairedDevices.isEmpty()) {
                    Text("暂无已配对设备。")
                } else {
                    pairedDevices.forEach { device ->
                        InfoCard(
                            title = device.pairedDeviceName,
                            body = "${device.pairedDeviceType} · ${device.pairingStatus}\nDevice ID：${device.pairedDeviceId}",
                        )
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    pairedDeviceRepository.delete(device.pairedDeviceId)
                                    pairedDevices = pairedDeviceRepository.getAll()
                                    status = "已解除本地配对：${device.pairedDeviceName}"
                                    busy = false
                                }
                            },
                        ) {
                            Text("解除配对")
                        }
                    }
                }
            }
        }

        InfoCard(title = "状态", body = status)
    }
}

private fun pairingErrorText(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        "TOKEN_EXPIRED" in message -> "二维码已过期，请在电脑端重新生成。"
        "TOKEN_INVALID" in message -> "配对码无效，请重新扫描电脑端二维码。"
        "ALREADY_PAIRED" in message -> "该电脑已经配对过，如需重配请先解除配对。"
        "Failed to connect" in message || "failed to connect" in message -> {
            "无法连接到电脑。请确认手机和电脑在同一网络；若使用公用 Wi-Fi，可能被防火墙或客户端隔离拦截。开发调试可使用 adb reverse。"
        }
        "timeout" in message.lowercase() -> "连接电脑超时，请确认电脑端二维码仍有效，且防火墙未拦截端口。"
        else -> message.ifBlank { "配对失败，请重试。" }
    }
}

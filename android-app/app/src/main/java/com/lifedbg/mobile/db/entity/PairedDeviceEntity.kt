package com.lifedbg.mobile.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "paired_device_id")
    val pairedDeviceId: String,
    @ColumnInfo(name = "paired_device_name")
    val pairedDeviceName: String,
    @ColumnInfo(name = "paired_device_type")
    val pairedDeviceType: String,
    @ColumnInfo(name = "public_key")
    val publicKey: String,
    @ColumnInfo(name = "last_known_host")
    val lastKnownHost: String?,
    @ColumnInfo(name = "last_known_port")
    val lastKnownPort: Int?,
    @ColumnInfo(name = "pairing_status")
    val pairingStatus: String,
    @ColumnInfo(name = "trust_level")
    val trustLevel: String,
    @ColumnInfo(name = "paired_at")
    val pairedAt: Long,
    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long?,
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: Long?,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String?,
)

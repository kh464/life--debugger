package com.lifedbg.mobile.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifedbg.mobile.db.dao.AppCategoryRuleDao
import com.lifedbg.mobile.db.dao.DeviceIdentityDao
import com.lifedbg.mobile.db.dao.LlmConfigDao
import com.lifedbg.mobile.db.dao.LlmReviewDao
import com.lifedbg.mobile.db.dao.ManualIntentDao
import com.lifedbg.mobile.db.dao.PairedDeviceDao
import com.lifedbg.mobile.db.dao.RawUsageEventDao
import com.lifedbg.mobile.db.dao.SummaryEventDao
import com.lifedbg.mobile.db.entity.AppCategoryRuleEntity
import com.lifedbg.mobile.db.entity.DeviceIdentityEntity
import com.lifedbg.mobile.db.entity.LlmConfigEntity
import com.lifedbg.mobile.db.entity.LlmReviewEntity
import com.lifedbg.mobile.db.entity.ManualIntentEntity
import com.lifedbg.mobile.db.entity.PairedDeviceEntity
import com.lifedbg.mobile.db.entity.RawUsageEventEntity
import com.lifedbg.mobile.db.entity.SummaryEventEntity

@Database(
    entities = [
        RawUsageEventEntity::class,
        SummaryEventEntity::class,
        AppCategoryRuleEntity::class,
        LlmConfigEntity::class,
        LlmReviewEntity::class,
        ManualIntentEntity::class,
        DeviceIdentityEntity::class,
        PairedDeviceEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class LifeDbgDatabase : RoomDatabase() {
    abstract fun rawUsageEventDao(): RawUsageEventDao
    abstract fun summaryEventDao(): SummaryEventDao
    abstract fun appCategoryRuleDao(): AppCategoryRuleDao
    abstract fun llmConfigDao(): LlmConfigDao
    abstract fun llmReviewDao(): LlmReviewDao
    abstract fun manualIntentDao(): ManualIntentDao
    abstract fun deviceIdentityDao(): DeviceIdentityDao
    abstract fun pairedDeviceDao(): PairedDeviceDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS device_identity (
                        device_id TEXT NOT NULL PRIMARY KEY,
                        device_name TEXT NOT NULL,
                        device_type TEXT NOT NULL,
                        public_key TEXT NOT NULL,
                        private_key_ref TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS paired_devices (
                        paired_device_id TEXT NOT NULL PRIMARY KEY,
                        paired_device_name TEXT NOT NULL,
                        paired_device_type TEXT NOT NULL,
                        public_key TEXT NOT NULL,
                        last_known_host TEXT,
                        last_known_port INTEGER,
                        pairing_status TEXT NOT NULL,
                        trust_level TEXT NOT NULL,
                        paired_at INTEGER NOT NULL,
                        last_seen_at INTEGER,
                        last_sync_at INTEGER,
                        metadata_json TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        fun create(context: Context): LifeDbgDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LifeDbgDatabase::class.java,
                "life_debugger.db",
            ).addMigrations(MIGRATION_1_2).build()
        }
    }
}

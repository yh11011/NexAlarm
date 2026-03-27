package com.nexalarm.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.model.RepeatDaysConverter

@Database(
    entities = [AlarmEntity::class, FolderEntity::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(RepeatDaysConverter::class)
abstract class NexAlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: NexAlarmDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeDelay INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE alarms ADD COLUMN maxSnoozeCount INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE alarms ADD COLUMN keepAfterRinging INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN emoji TEXT NOT NULL DEFAULT '📁'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE folders SET emoji = '↻' WHERE name = '重複鬧鐘' AND isSystem = 1")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 新增雲端同步欄位：clientId（UUID）和 updatedAt（Unix ms）
                db.execSQL("ALTER TABLE alarms ADD COLUMN clientId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                // 為現有鬧鐘產生 UUID 和時間戳記（注意：此格式無破折號，由 MIGRATION_6_7 修復）
                db.execSQL("UPDATE alarms SET clientId = lower(hex(randomblob(16))), updatedAt = createdAt WHERE clientId = ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 修復 clientId 格式不一致問題：
                // MIGRATION_5_6 產生的 clientId 為 32 字符無破折號（e.g. 550e8400e29b41d4a716446655440000）
                // AlarmEntity 預設值使用 UUID.randomUUID() 產生的為 36 字符帶破折號（e.g. 550e8400-e29b-41d4-a716-446655440000）
                // 此遷移將所有 32 字符的 clientId 轉換為標準 UUID 格式（帶破折號）
                db.execSQL("""
                    UPDATE alarms
                    SET clientId = LOWER(
                        SUBSTR(clientId, 1, 8) || '-' ||
                        SUBSTR(clientId, 9, 4) || '-' ||
                        SUBSTR(clientId, 13, 4) || '-' ||
                        SUBSTR(clientId, 17, 4) || '-' ||
                        SUBSTR(clientId, 21, 12)
                    )
                    WHERE LENGTH(clientId) = 32
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): NexAlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexAlarmDatabase::class.java,
                    "nexalarm_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(PrepopulateCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Use raw SQL to avoid race condition with INSTANCE being null
            db.execSQL(
                "INSERT INTO folders (name, isEnabled, color, isSystem, emoji) VALUES ('單次鬧鐘', 1, '#4CAF50', 1, '🔔')"
            )
            db.execSQL(
                "INSERT INTO folders (name, isEnabled, color, isSystem, emoji) VALUES ('重複鬧鐘', 1, '#FF9800', 1, '↻')"
            )
        }
    }
}

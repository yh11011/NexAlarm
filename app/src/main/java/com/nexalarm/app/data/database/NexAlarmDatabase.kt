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
    version = 5,
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
                // 將系統資料夾「重複鬧鐘」的 emoji 從 🔁（Samsung 裝置上有橘色底色）改為純文字箭頭 ↻
                db.execSQL("UPDATE folders SET emoji = '↻' WHERE name = '重複鬧鐘' AND isSystem = 1")
            }
        }

        fun getDatabase(context: Context): NexAlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexAlarmDatabase::class.java,
                    "nexalarm_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

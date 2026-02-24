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
    version = 2,
    exportSchema = false
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
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeDelay INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE alarms ADD COLUMN maxSnoozeCount INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE alarms ADD COLUMN keepAfterRinging INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): NexAlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexAlarmDatabase::class.java,
                    "nexalarm_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
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
                "INSERT INTO folders (name, isEnabled, color, isSystem) VALUES ('Single Alarm', 1, '#4CAF50', 1)"
            )
            db.execSQL(
                "INSERT INTO folders (name, isEnabled, color, isSystem) VALUES ('Recurring Alarm', 1, '#FF9800', 1)"
            )
        }
    }
}

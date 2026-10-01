package com.nexalarm.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.nexalarm.app.data.database.NexAlarmDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DateMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), NexAlarmDatabase::class.java)

    @Test fun migrationPreservesAlarmAndAddsLocalDateDefaults() {
        val name = "date-migration-test"
        helper.createDatabase(name, 8).apply {
            execSQL("INSERT INTO alarms(id,hour,minute,title,isEnabled,isRecurring,repeatDays,folderId,vibrateOnly,volume,ringtoneUri,snoozeDelay,maxSnoozeCount,keepAfterRinging,snoozeEnabled,createdAt,clientId,updatedAt,is_deleted) VALUES(1,7,30,'Wake up',0,1,'1,2,3,4,5',NULL,0,80,'',10,3,0,1,100,'stable-client-id',100,0)")
            close()
        }
        helper.runMigrationsAndValidate(name, 9, true, NexAlarmDatabase.MIGRATION_8_9).use { db ->
            db.query("SELECT title,isEnabled,repeatDays,clientId,scheduledDate,timePolicy FROM alarms WHERE id=1").use {
                assertTrue(it.moveToFirst())
                assertEquals("Wake up", it.getString(0))
                assertEquals(0, it.getInt(1))
                assertEquals("1,2,3,4,5", it.getString(2))
                assertEquals("stable-client-id", it.getString(3))
                assertTrue(it.isNull(4))
                assertEquals("device_local", it.getString(5))
            }
        }
    }
}

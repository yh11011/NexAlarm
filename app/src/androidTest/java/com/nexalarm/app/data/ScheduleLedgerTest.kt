package com.nexalarm.app.data

import androidx.test.platform.app.InstrumentationRegistry
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.util.AlarmScheduler
import org.junit.Assert.*
import org.junit.Test

class ScheduleLedgerTest {
    @Test fun persistsEvidenceAndRejectsPreviousAlarmVersion() {
        // Use the isolated instrumentation APK's storage, never the target app's alarms.
        val context = InstrumentationRegistry.getInstrumentation().context
        val alarm = AlarmEntity(hour = 7, minute = 30, clientId = "isolated-ledger-test", updatedAt = 100)
        val ledger = ScheduleLedger(context)
        ledger.record(alarm, AlarmScheduler.ScheduleResult("scheduled", 1900000000000))
        assertEquals("scheduled", ScheduleLedger(context).current(alarm)!!.getString("status"))
        assertNull(ledger.current(alarm.copy(updatedAt = 101)))
        assertEquals("unknown", ledger.report(alarm.copy(updatedAt = 101)).getString("status"))
        ledger.record(alarm, AlarmScheduler.ScheduleResult("cancelled"))
        assertEquals("cancelled", ledger.current(alarm)!!.getString("status"))
        assertTrue(ledger.current(alarm)!!.isNull("trigger_at"))
    }
}

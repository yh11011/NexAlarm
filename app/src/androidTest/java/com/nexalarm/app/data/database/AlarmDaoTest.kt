package com.nexalarm.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexalarm.app.data.model.AlarmEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AlarmDaoTest {

    private lateinit var db: NexAlarmDatabase
    private lateinit var alarmDao: AlarmDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NexAlarmDatabase::class.java).build()
        alarmDao = db.alarmDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveAlarm() = runTest {
        val alarm = createTestAlarm(hour = 7, minute = 30, title = "Morning")
        val alarmId = alarmDao.insert(alarm)

        val retrieved = alarmDao.getAlarmById(alarmId)
        assertNotNull(retrieved)
        assertEquals("Morning", retrieved?.title)
        assertEquals(7, retrieved?.hour)
        assertEquals(30, retrieved?.minute)
    }

    @Test
    fun updateAlarmAndVerifyChanges() = runTest {
        val alarm = createTestAlarm(hour = 8, minute = 0, title = "Old Title")
        val alarmId = alarmDao.insert(alarm)

        val updated = alarm.copy(id = alarmId, title = "New Title", hour = 9, minute = 30)
        alarmDao.update(updated)

        val retrieved = alarmDao.getAlarmById(alarmId)
        assertEquals("New Title", retrieved?.title)
        assertEquals(9, retrieved?.hour)
        assertEquals(30, retrieved?.minute)
    }

    @Test
    fun deleteAlarmAndVerifyRemoval() = runTest {
        val alarm = createTestAlarm(hour = 10, minute = 0, title = "To Delete")
        val alarmId = alarmDao.insert(alarm)

        // 驗證插入成功
        assertNotNull(alarmDao.getAlarmById(alarmId))

        // 刪除
        alarmDao.deleteById(alarmId)

        // 驗證刪除成功
        assertNull(alarmDao.getAlarmById(alarmId))
    }

    @Test
    fun getAlarmsByFolder() = runTest {
        val folder1Id = 1L
        val folder2Id = 2L

        // 在不同資料夾插入鬧鐘
        alarmDao.insert(createTestAlarm(hour = 7, minute = 0, folderId = folder1Id))
        alarmDao.insert(createTestAlarm(hour = 8, minute = 0, folderId = folder1Id))
        alarmDao.insert(createTestAlarm(hour = 9, minute = 0, folderId = folder2Id))

        // 獲取 folder1 的鬧鐘
        val folder1Alarms = alarmDao.getAlarmsByFolder(folder1Id).first()
        assertEquals(2, folder1Alarms.size)

        // 獲取 folder2 的鬧鐘
        val folder2Alarms = alarmDao.getAlarmsByFolder(folder2Id).first()
        assertEquals(1, folder2Alarms.size)
    }

    @Test
    fun getEnabledAlarmsOnly() = runTest {
        // 插入啟用和禁用的鬧鐘
        alarmDao.insert(createTestAlarm(hour = 7, minute = 0, isEnabled = true))
        alarmDao.insert(createTestAlarm(hour = 8, minute = 0, isEnabled = false))
        alarmDao.insert(createTestAlarm(hour = 9, minute = 0, isEnabled = true))

        val enabledAlarms = alarmDao.getEnabledAlarms().first()
        assertEquals(2, enabledAlarms.size)
        assertTrue(enabledAlarms.all { it.isEnabled })
    }

    @Test
    fun findDuplicateAlarm() = runTest {
        val hour = 7
        val minute = 30
        val title = "Duplicate Test"
        val folderId = 1L
        val repeatDaysString = "1,2,3"
        val repeatDays = listOf(1, 2, 3)

        // 插入第一個鬧鐘
        alarmDao.insert(createTestAlarm(
            hour = hour,
            minute = minute,
            title = title,
            folderId = folderId,
            repeatDays = repeatDays
        ))

        // 嘗試查找重複（使用字符串格式，模擬資料庫持久化狀態）
        val duplicate = alarmDao.findDuplicate(hour, minute, title, folderId, repeatDaysString)
        assertNotNull(duplicate)
        assertEquals(title, duplicate?.title)
    }

    @Test
    fun findDuplicateWithNullFolderId() = runTest {
        val hour = 8
        val minute = 0
        val title = "Null Folder Test"
        val repeatDays = listOf(4, 5)
        val repeatDaysString = "4,5"

        // 插入無資料夾的鬧鐘
        alarmDao.insert(createTestAlarm(
            hour = hour,
            minute = minute,
            title = title,
            folderId = null,
            repeatDays = repeatDays
        ))

        // 查找重複（folderId 為 null）
        val duplicate = alarmDao.findDuplicate(hour, minute, title, null, repeatDaysString)
        assertNotNull(duplicate)
        assertEquals(title, duplicate?.title)
        assertNull(duplicate?.folderId)
    }

    @Test
    fun setAlarmEnabledStatus() = runTest {
        val alarm = createTestAlarm(hour = 7, minute = 0, isEnabled = false)
        val alarmId = alarmDao.insert(alarm)

        // 驗證初始狀態
        var retrieved = alarmDao.getAlarmById(alarmId)
        assertFalse(retrieved?.isEnabled ?: true)

        // 啟用鬧鐘
        alarmDao.setEnabled(alarmId, true)

        // 驗證更新後的狀態
        retrieved = alarmDao.getAlarmById(alarmId)
        assertTrue(retrieved?.isEnabled ?: false)
    }

    @Test
    fun setVibrateOnlyMode() = runTest {
        val alarm = createTestAlarm(hour = 7, minute = 0, vibrateOnly = false)
        val alarmId = alarmDao.insert(alarm)

        // 驗證初始狀態
        var retrieved = alarmDao.getAlarmById(alarmId)
        assertFalse(retrieved?.vibrateOnly ?: true)

        // 設定為僅震動
        alarmDao.setVibrateOnly(alarmId, true)

        // 驗證更新後的狀態
        retrieved = alarmDao.getAlarmById(alarmId)
        assertTrue(retrieved?.vibrateOnly ?: false)
    }

    @Test
    fun getTodayAlarms() = runTest {
        // 星期一（Calendar 格式為 1=週日，2=週一，但我們內部格式 1=週一）
        val monday = 1

        // 插入不同重複日的鬧鐘
        alarmDao.insert(createTestAlarm(
            hour = 7,
            minute = 0,
            isRecurring = true,
            repeatDays = listOf(monday, 2, 3)  // 週一、週二、週三
        ))
        alarmDao.insert(createTestAlarm(
            hour = 8,
            minute = 0,
            isRecurring = true,
            repeatDays = listOf(4, 5, 6)  // 週四、週五、週六
        ))
        alarmDao.insert(createTestAlarm(
            hour = 9,
            minute = 0,
            isRecurring = false  // 單次鬧鐘
        ))

        // 獲取週一的鬧鐘（應該包含單次鬧鐘和週一重複的鬧鐘）
        val todayAlarms = alarmDao.getTodayAlarms(monday)
        assertEquals(2, todayAlarms.size)
    }

    @Test
    fun getAlarmCountByFolder() = runTest {
        val folder1Id = 1L
        val folder2Id = 2L

        // 在 folder1 插入 3 個鬧鐘
        repeat(3) {
            alarmDao.insert(createTestAlarm(hour = 7 + it, minute = 0, folderId = folder1Id))
        }

        // 在 folder2 插入 2 個鬧鐘
        repeat(2) {
            alarmDao.insert(createTestAlarm(hour = 10 + it, minute = 0, folderId = folder2Id))
        }

        // 驗證計數
        assertEquals(3, alarmDao.getAlarmCountByFolder(folder1Id))
        assertEquals(2, alarmDao.getAlarmCountByFolder(folder2Id))
    }

    @Test
    fun getTotalAlarmCount() = runTest {
        // 初始計數應為 0
        assertEquals(0, alarmDao.getTotalAlarmCount())

        // 插入 5 個鬧鐘
        repeat(5) {
            alarmDao.insert(createTestAlarm(hour = 7 + it, minute = 0))
        }

        // 驗證總計數
        assertEquals(5, alarmDao.getTotalAlarmCount())
    }

    @Test
    fun findTimeConflictInSameFolder() = runTest {
        val folderId = 1L
        val hour = 7
        val minute = 30

        // 插入第一個鬧鐘
        val firstAlarmId = alarmDao.insert(createTestAlarm(
            hour = hour,
            minute = minute,
            folderId = folderId
        ))

        // 嘗試插入相同時間的鬧鐘
        val conflict = alarmDao.findTimeConflict(hour, minute, folderId, firstAlarmId)
        assertNull(conflict)  // 排除自身，不應該找到衝突

        // 插入第二個相同時間的鬧鐘
        val secondAlarmId = alarmDao.insert(createTestAlarm(
            hour = hour,
            minute = minute,
            folderId = folderId
        ))

        // 現在應該能找到衝突
        val conflict2 = alarmDao.findTimeConflict(hour, minute, folderId, secondAlarmId)
        assertNotNull(conflict2)
        assertEquals(firstAlarmId, conflict2?.id)
    }

    @Test
    fun getAlarmByClientId() = runTest {
        val clientId = "test-client-123"
        val alarm = createTestAlarm(hour = 7, minute = 0, clientId = clientId)
        val alarmId = alarmDao.insert(alarm)

        val retrieved = alarmDao.getByClientId(clientId)
        assertNotNull(retrieved)
        assertEquals(alarmId, retrieved?.id)
        assertEquals(clientId, retrieved?.clientId)
    }

    @Test
    fun deleteMultipleAlarms() = runTest {
        val alarm1 = createTestAlarm(hour = 7, minute = 0)
        val alarm2 = createTestAlarm(hour = 8, minute = 0)
        val alarm3 = createTestAlarm(hour = 9, minute = 0)

        val id1 = alarmDao.insert(alarm1)
        val id2 = alarmDao.insert(alarm2)
        val id3 = alarmDao.insert(alarm3)

        // 驗證全部存在
        assertEquals(3, alarmDao.getTotalAlarmCount())

        // 批次刪除
        val alarmsToDelete = listOf(
            alarmDao.getAlarmById(id1)!!,
            alarmDao.getAlarmById(id2)!!
        )
        alarmDao.deleteAll(alarmsToDelete)

        // 驗證只剩下一個
        assertEquals(1, alarmDao.getTotalAlarmCount())
        assertNotNull(alarmDao.getAlarmById(id3))
    }

    @Test
    fun getAllAlarmsOrderedByTime() = runTest {
        // 插入不同時間的鬧鐘（亂序）
        alarmDao.insert(createTestAlarm(hour = 9, minute = 0))
        alarmDao.insert(createTestAlarm(hour = 7, minute = 30))
        alarmDao.insert(createTestAlarm(hour = 8, minute = 15))

        // 獲取所有鬧鐘
        val allAlarms = alarmDao.getAllAlarms().first()

        // 驗證按時間排序
        assertEquals(3, allAlarms.size)
        assertEquals(7, allAlarms[0].hour)
        assertEquals(30, allAlarms[0].minute)
        assertEquals(8, allAlarms[1].hour)
        assertEquals(15, allAlarms[1].minute)
        assertEquals(9, allAlarms[2].hour)
        assertEquals(0, allAlarms[2].minute)
    }

    @Test
    fun softDeleteWithIsDeletedFlag() = runTest {
        val alarm = createTestAlarm(hour = 7, minute = 0, isDeleted = false)
        val alarmId = alarmDao.insert(alarm)

        // 驗證存在於 getAllAlarms
        assertEquals(1, alarmDao.getAllAlarms().first().size)

        // 軟刪除
        val deletedAlarm = alarmDao.getAlarmById(alarmId)!!.copy(isDeleted = true)
        alarmDao.update(deletedAlarm)

        // 驗證不再出現在 getAllAlarms
        assertEquals(0, alarmDao.getAllAlarms().first().size)

        // 但仍然存在於 getAllAlarmsList（包含已刪除的）
        assertEquals(1, alarmDao.getAllAlarmsList().size)
    }

    // 輔助方法：創建測試鬧鐘
    private fun createTestAlarm(
        hour: Int = 7,
        minute: Int = 0,
        title: String = "Test Alarm",
        isEnabled: Boolean = true,
        isRecurring: Boolean = false,
        repeatDays: List<Int> = emptyList(),
        folderId: Long? = null,
        vibrateOnly: Boolean = false,
        isDeleted: Boolean = false,
        clientId: String = "client-${System.currentTimeMillis()}"
    ): AlarmEntity {
        return AlarmEntity(
            id = 0,  // 0 表示新插入
            hour = hour,
            minute = minute,
            title = title,
            isEnabled = isEnabled,
            isRecurring = isRecurring,
            repeatDays = repeatDays,
            folderId = folderId,
            ringtoneUri = "",
            vibrateOnly = vibrateOnly,
            snoozeEnabled = false,
            snoozeDelay = 5,
            isDeleted = isDeleted,
            clientId = clientId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

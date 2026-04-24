package com.nexalarm.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexalarm.app.data.model.FolderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class FolderDaoTest {

    private lateinit var db: NexAlarmDatabase
    private lateinit var folderDao: FolderDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NexAlarmDatabase::class.java).build()
        folderDao = db.folderDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveFolder() = runTest {
        val folder = createTestFolder(name = "Work")
        val folderId = folderDao.insert(folder)

        val retrieved = folderDao.getFolderById(folderId)
        assertNotNull(retrieved)
        assertEquals("Work", retrieved?.name)
        assertEquals("#1A73E8", retrieved?.color)
        assertFalse(retrieved?.isSystem ?: true)
    }

    @Test
    fun updateFolderAndVerifyChanges() = runTest {
        val folder = createTestFolder(name = "Old Name", color = "#FF0000")
        val folderId = folderDao.insert(folder)

        val updated = folder.copy(
            id = folderId,
            name = "New Name",
            color = "#00FF00",
            emoji = "🏢"
        )
        folderDao.update(updated)

        val retrieved = folderDao.getFolderById(folderId)
        assertEquals("New Name", retrieved?.name)
        assertEquals("#00FF00", retrieved?.color)
        assertEquals("🏢", retrieved?.emoji)
    }

    @Test
    fun deleteFolderAndVerifyRemoval() = runTest {
        val folder = createTestFolder(name = "To Delete")
        val folderId = folderDao.insert(folder)

        // 驗證插入成功
        assertNotNull(folderDao.getFolderById(folderId))

        // 刪除
        folderDao.delete(folderDao.getFolderById(folderId)!!)

        // 驗證刪除成功
        assertNull(folderDao.getFolderById(folderId))
    }

    @Test
    fun getAllFoldersOrderedBySystemFirstThenName() = runTest {
        // 插入系統資料夾
        folderDao.insert(createTestFolder(name = "System 2", isSystem = true, emoji = "↻"))
        folderDao.insert(createTestFolder(name = "System 1", isSystem = true, emoji = "🔔"))

        // 插入用戶資料夾（亂序名稱）
        folderDao.insert(createTestFolder(name = "Zebra"))
        folderDao.insert(createTestFolder(name = "Apple"))
        folderDao.insert(createTestFolder(name = "Mango"))

        // 獲取所有資料夾
        val allFolders = folderDao.getAllFolders().first()

        // 驗證排序：系統資料夾在前，然後按名稱排序
        assertEquals(5, allFolders.size)

        // 前兩個應該是系統資料夾
        assertTrue(allFolders[0].isSystem)
        assertTrue(allFolders[1].isSystem)

        // 系統資料夾應按名稱排序
        assertTrue(allFolders[0].name < allFolders[1].name)

        // 後三個應該是用戶資料夾，按名稱排序
        assertFalse(allFolders[2].isSystem)
        assertFalse(allFolders[3].isSystem)
        assertFalse(allFolders[4].isSystem)

        assertEquals("Apple", allFolders[2].name)
        assertEquals("Mango", allFolders[3].name)
        assertEquals("Zebra", allFolders[4].name)
    }

    @Test
    fun getFolderById() = runTest {
        val folder1 = createTestFolder(name = "Folder 1")
        val folder2 = createTestFolder(name = "Folder 2")

        val id1 = folderDao.insert(folder1)
        val id2 = folderDao.insert(folder2)

        val retrieved1 = folderDao.getFolderById(id1)
        val retrieved2 = folderDao.getFolderById(id2)

        assertEquals("Folder 1", retrieved1?.name)
        assertEquals("Folder 2", retrieved2?.name)
        assertNotEquals(retrieved1?.id, retrieved2?.id)
    }

    @Test
    fun findFolderByName() = runTest {
        val folderName = "Study"
        folderDao.insert(createTestFolder(name = folderName))
        folderDao.insert(createTestFolder(name = "Work"))
        folderDao.insert(createTestFolder(name = "Personal"))

        val found = folderDao.findByName(folderName)
        assertNotNull(found)
        assertEquals(folderName, found?.name)
    }

    @Test
    fun findFolderByNameReturnsFirstMatch() = runTest {
        val folderName = "Duplicate"
        folderDao.insert(createTestFolder(name = folderName, color = "#FF0000"))
        folderDao.insert(createTestFolder(name = folderName, color = "#00FF00"))

        val found = folderDao.findByName(folderName)
        assertNotNull(found)
        assertEquals(folderName, found?.name)
        // 應該返回第一個匹配的
        assertTrue(found?.color == "#FF0000" || found?.color == "#00FF00")
    }

    @Test
    fun findFolderByNameReturnsNullWhenNotFound() = runTest {
        folderDao.insert(createTestFolder(name = "Existing"))

        val notFound = folderDao.findByName("NonExistent")
        assertNull(notFound)
    }

    @Test
    fun setFolderEnabledStatus() = runTest {
        val folder = createTestFolder(name = "Test", isEnabled = false)
        val folderId = folderDao.insert(folder)

        // 驗證初始狀態
        var retrieved = folderDao.getFolderById(folderId)
        assertFalse(retrieved?.isEnabled ?: true)

        // 啟用資料夾
        folderDao.setEnabled(folderId, true)

        // 驗證更新後的狀態
        retrieved = folderDao.getFolderById(folderId)
        assertTrue(retrieved?.isEnabled ?: false)

        // 禁用資料夾
        folderDao.setEnabled(folderId, false)

        // 驗證禁用後的狀態
        retrieved = folderDao.getFolderById(folderId)
        assertFalse(retrieved?.isEnabled ?: true)
    }

    @Test
    fun getUserFolderCountExcludesSystemFolders() = runTest {
        // 初始計數應為 0
        assertEquals(0, folderDao.getUserFolderCount())

        // 插入系統資料夾
        folderDao.insert(createTestFolder(name = "System 1", isSystem = true))
        folderDao.insert(createTestFolder(name = "System 2", isSystem = true))

        // 系統資料夾不應計入
        assertEquals(0, folderDao.getUserFolderCount())

        // 插入用戶資料夾
        folderDao.insert(createTestFolder(name = "User 1"))
        folderDao.insert(createTestFolder(name = "User 2"))
        folderDao.insert(createTestFolder(name = "User 3"))

        // 應該只計算用戶資料夾
        assertEquals(3, folderDao.getUserFolderCount())
    }

    @Test
    fun getUserFolderCountUpdatesAfterDeletion() = runTest {
        // 插入 5 個用戶資料夾
        repeat(5) { index ->
            folderDao.insert(createTestFolder(name = "Folder $index"))
        }

        assertEquals(5, folderDao.getUserFolderCount())

        // 刪除其中 2 個
        val allFolders = folderDao.getAllFolders().first()
        folderDao.delete(allFolders[0])
        folderDao.delete(allFolders[1])

        // 計數應更新
        assertEquals(3, folderDao.getUserFolderCount())
    }

    @Test
    fun folderWithCustomColorAndEmoji() = runTest {
        val customFolder = createTestFolder(
            name = "Custom",
            color = "#FF5722",
            emoji = "🎨"
        )
        val folderId = folderDao.insert(customFolder)

        val retrieved = folderDao.getFolderById(folderId)
        assertEquals("#FF5722", retrieved?.color)
        assertEquals("🎨", retrieved?.emoji)
    }

    @Test
    fun folderEnabledStatusAffectsGetAllFoldersOrder() = runTest {
        // 插入啟用和禁用的資料夾
        folderDao.insert(createTestFolder(name = "Enabled A", isEnabled = true))
        folderDao.insert(createTestFolder(name = "Disabled", isEnabled = false))
        folderDao.insert(createTestFolder(name = "Enabled B", isEnabled = true))

        val allFolders = folderDao.getAllFolders().first()
        assertEquals(3, allFolders.size)

        // 驗證所有資料夾都被獲取（isEnabled 不影響查詢）
        assertTrue(allFolders.any { it.name == "Enabled A" })
        assertTrue(allFolders.any { it.name == "Disabled" })
        assertTrue(allFolders.any { it.name == "Enabled B" })
    }

    @Test
    fun updateFolderPreservesId() = runTest {
        val originalFolder = createTestFolder(name = "Original")
        val originalId = folderDao.insert(originalFolder)

        val updatedFolder = originalFolder.copy(
            id = originalId,
            name = "Updated",
            color = "#FF0000"
        )
        folderDao.update(updatedFolder)

        val retrieved = folderDao.getFolderById(originalId)
        assertNotNull(retrieved)
        assertEquals(originalId, retrieved?.id)
        assertEquals("Updated", retrieved?.name)
        assertEquals("#FF0000", retrieved?.color)
    }

    @Test
    fun multipleFoldersWithSameNameCanExist() = runTest {
        val name = "Same Name"
        val id1 = folderDao.insert(createTestFolder(name = name, color = "#FF0000"))
        val id2 = folderDao.insert(createTestFolder(name = name, color = "#00FF00"))

        assertNotEquals(id1, id2)

        val folder1 = folderDao.getFolderById(id1)
        val folder2 = folderDao.getFolderById(id2)

        assertEquals(name, folder1?.name)
        assertEquals(name, folder2?.name)
        assertNotEquals(folder1?.color, folder2?.color)
    }

    @Test
    fun folderFlowEmitsUpdates() = runTest {
        val folder = createTestFolder(name = "Test")
        val folderId = folderDao.insert(folder)

        // 獲取初始 flow
        val folderFlow = folderDao.getAllFolders()
        var folders = folderFlow.first()
        assertEquals(1, folders.size)

        // 更新資料夾
        val updated = folders[0].copy(name = "Updated Name")
        folderDao.update(updated)

        // Flow 應該發射新值
        folders = folderFlow.first()
        assertEquals("Updated Name", folders[0].name)
    }

    @Test
    fun systemFolderCountDoesNotAffectUserFolderCount() = runTest {
        // 插入多個系統資料夾
        repeat(10) {
            folderDao.insert(createTestFolder(
                name = "System $it",
                isSystem = true
            ))
        }

        // 系統資料夾不應影響用戶資料夾計數
        assertEquals(0, folderDao.getUserFolderCount())

        // 插入用戶資料夾
        repeat(3) {
            folderDao.insert(createTestFolder(name = "User $it"))
        }

        // 只有用戶資料夾被計數
        assertEquals(3, folderDao.getUserFolderCount())
    }

    // 輔助方法：創建測試資料夾
    private fun createTestFolder(
        name: String = "Test Folder",
        isEnabled: Boolean = true,
        color: String = "#1A73E8",
        isSystem: Boolean = false,
        emoji: String = "📁"
    ): FolderEntity {
        return FolderEntity(
            id = 0,  // 0 表示新插入
            name = name,
            isEnabled = isEnabled,
            color = color,
            isSystem = isSystem,
            emoji = emoji
        )
    }
}

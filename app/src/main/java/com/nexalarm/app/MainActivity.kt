package com.nexalarm.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.data.repository.FolderRepository
import com.nexalarm.app.ui.screens.AlarmEditScreen
import com.nexalarm.app.ui.screens.FolderManageScreen
import com.nexalarm.app.ui.screens.HomeScreen
import com.nexalarm.app.ui.theme.NexAlarmTheme
import com.nexalarm.app.viewmodel.AlarmViewModel
import com.nexalarm.app.viewmodel.FolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        handleDeepLink(intent)

        setContent {
            NexAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NexAlarmNavHost()
                }
            }
        }
    }

    private fun requestPermissions() {
        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "nexalarm") return

        val action = uri.host ?: uri.path?.trimStart('/') ?: return

        val db = NexAlarmDatabase.getDatabase(this)
        val alarmRepo = AlarmRepository(db.alarmDao())
        val folderRepo = FolderRepository(db.folderDao())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (action) {
                    "add" -> handleUriAdd(uri, alarmRepo, folderRepo)
                    "delete" -> handleUriDelete(uri, alarmRepo)
                    "toggle_folder" -> handleUriToggleFolder(uri, folderRepo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleUriAdd(uri: Uri, alarmRepo: AlarmRepository, folderRepo: FolderRepository) {
        val timeStr = uri.getQueryParameter("time") ?: return
        val hour = timeStr.take(2).toIntOrNull() ?: return
        val minute = timeStr.drop(2).toIntOrNull() ?: return
        val title = uri.getQueryParameter("title") ?: ""
        val folderName = uri.getQueryParameter("folder")
        val repeatStr = uri.getQueryParameter("repeat")
        val silent = uri.getQueryParameter("silent")?.toBooleanStrictOrNull() ?: false

        val folderId = if (folderName != null) {
            val folder = folderRepo.findByName(folderName)
            folder?.id ?: folderRepo.insert(FolderEntity(name = folderName))
        } else null

        val repeatDays = repeatStr?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
        val isRecurring = repeatDays.isNotEmpty()

        val alarm = AlarmEntity(
            hour = hour,
            minute = minute,
            title = title,
            isEnabled = true,
            isRecurring = isRecurring,
            repeatDays = repeatDays,
            folderId = folderId,
            vibrateOnly = silent
        )

        alarmRepo.insertOrUpdate(alarm)

        runOnUiThread {
            Toast.makeText(this, "Alarm added: $title ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun handleUriDelete(uri: Uri, alarmRepo: AlarmRepository) {
        val idStr = uri.getQueryParameter("id") ?: return
        val id = idStr.toLongOrNull() ?: return
        alarmRepo.deleteById(id)

        runOnUiThread {
            Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun handleUriToggleFolder(uri: Uri, folderRepo: FolderRepository) {
        val folderName = uri.getQueryParameter("name") ?: return
        val folder = folderRepo.findByName(folderName) ?: return
        folderRepo.setEnabled(folder.id, !folder.isEnabled)

        runOnUiThread {
            Toast.makeText(this, "Folder '${folder.name}' toggled", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun NexAlarmNavHost() {
    val navController = rememberNavController()
    val alarmViewModel: AlarmViewModel = viewModel()
    val folderViewModel: FolderViewModel = viewModel()

    val alarms by alarmViewModel.filteredAlarms.collectAsState()
    val folders by folderViewModel.allFolders.collectAsState()
    val selectedFolderId by alarmViewModel.selectedFolderId.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                alarms = alarms,
                folders = folders,
                selectedFolderId = selectedFolderId,
                onFolderSelected = { alarmViewModel.setFolderFilter(it) },
                onAddAlarmClick = { navController.navigate("alarm_edit/-1") },
                onAlarmClick = { alarm -> navController.navigate("alarm_edit/${alarm.id}") },
                onAlarmToggle = { alarm -> alarmViewModel.toggleAlarm(alarm) },
                onAlarmDelete = { alarm -> alarmViewModel.deleteAlarm(alarm) },
                onFolderManageClick = { navController.navigate("folder_manage") }
            )
        }

        composable(
            route = "alarm_edit/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
            var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
            var isLoading by remember { mutableStateOf(alarmId > 0) }

            LaunchedEffect(alarmId) {
                if (alarmId > 0) {
                    alarm = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        alarmViewModel.getAlarmById(alarmId)
                    }
                }
                isLoading = false
            }

            if (!isLoading) {
                AlarmEditScreen(
                    alarm = if (alarmId > 0) alarm else null,
                    folders = folders,
                    onSave = { result ->
                        if (alarmId > 0) {
                            alarmViewModel.updateAlarm(result)
                        } else {
                            alarmViewModel.saveAlarm(result)
                        }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("folder_manage") {
            FolderManageScreen(
                folders = folders,
                onAddFolder = { name, color -> folderViewModel.addFolder(name, color) },
                onUpdateFolder = { folder -> folderViewModel.updateFolder(folder) },
                onDeleteFolder = { folder -> folderViewModel.deleteFolder(folder) },
                onToggleFolder = { id -> folderViewModel.toggleFolder(id) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

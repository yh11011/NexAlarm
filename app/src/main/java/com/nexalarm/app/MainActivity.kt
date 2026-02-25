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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexalarm.app.data.database.NexAlarmDatabase
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.data.model.FolderEntity
import com.nexalarm.app.data.repository.AlarmRepository
import com.nexalarm.app.data.repository.FolderRepository
import com.nexalarm.app.ui.screens.*
import com.nexalarm.app.ui.theme.*
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
                NexAlarmMainContent()
            }
        }
    }

    private fun requestPermissions() {
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
            hour = hour, minute = minute, title = title, isEnabled = true,
            isRecurring = isRecurring, repeatDays = repeatDays,
            folderId = folderId, vibrateOnly = silent
        )
        alarmRepo.insertOrUpdate(alarm)

        runOnUiThread {
            Toast.makeText(this, "Alarm added: $title ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun handleUriDelete(uri: Uri, alarmRepo: AlarmRepository) {
        val id = uri.getQueryParameter("id")?.toLongOrNull() ?: return
        alarmRepo.deleteById(id)
        runOnUiThread { Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show() }
    }

    private suspend fun handleUriToggleFolder(uri: Uri, folderRepo: FolderRepository) {
        val folderName = uri.getQueryParameter("name") ?: return
        val folder = folderRepo.findByName(folderName) ?: return
        folderRepo.setEnabled(folder.id, !folder.isEnabled)
        runOnUiThread { Toast.makeText(this, "Folder '${folder.name}' toggled", Toast.LENGTH_SHORT).show() }
    }
}

// ── Bottom Nav Items ──
enum class BottomTab(val label: String, val icon: ImageVector, val route: String) {
    SINGLE("單次", Icons.Default.Notifications, "single"),
    REPEAT("多次", Icons.Default.NotificationsActive, "repeat"),
    FOLDERS("資料夾", Icons.Default.Folder, "folders"),
    STOPWATCH("碼錶", Icons.Default.Timer, "stopwatch"),
    TIMER("計時器", Icons.Default.HourglassBottom, "timer")
}

@Composable
fun NexAlarmMainContent() {
    val navController = rememberNavController()
    val alarmViewModel: AlarmViewModel = viewModel()
    val folderViewModel: FolderViewModel = viewModel()

    val alarms by alarmViewModel.allAlarms.collectAsState()
    val folders by folderViewModel.allFolders.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on main tabs
    val showBottomBar = currentRoute in BottomTab.entries.map { it.route }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkBackground,
                    tonalElevation = 0.dp,
                    modifier = Modifier.background(DarkBackground)
                ) {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(tab.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextPrimary,
                                selectedTextColor = TextPrimary,
                                unselectedIconColor = TextSecondary.copy(alpha = 0.45f),
                                unselectedTextColor = TextSecondary.copy(alpha = 0.45f),
                                indicatorColor = DarkBackground
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.SINGLE.route,
            modifier = Modifier.padding(padding)
        ) {
            // Tab: Single alarms
            composable(BottomTab.SINGLE.route) {
                SingleAlarmScreen(
                    alarms = alarms,
                    folders = folders,
                    onAddClick = { navController.navigate("alarm_edit/-1") },
                    onAlarmClick = { alarm -> navController.navigate("alarm_edit/${alarm.id}") },
                    onAlarmToggle = { alarm -> alarmViewModel.toggleAlarm(alarm) }
                )
            }

            // Tab: Repeat alarms
            composable(BottomTab.REPEAT.route) {
                RepeatAlarmScreen(
                    alarms = alarms,
                    folders = folders,
                    onAddClick = { navController.navigate("alarm_edit/-1") },
                    onAlarmClick = { alarm -> navController.navigate("alarm_edit/${alarm.id}") },
                    onAlarmToggle = { alarm -> alarmViewModel.toggleAlarm(alarm) }
                )
            }

            // Tab: Folders
            composable(BottomTab.FOLDERS.route) {
                // Build alarm count map
                val alarmCountMap = remember(alarms, folders) {
                    folders.associate { folder ->
                        folder.id to alarms.count { it.folderId == folder.id }
                    }
                }
                FolderManageScreen(
                    folders = folders,
                    alarmCountMap = alarmCountMap,
                    onAddFolder = { name, color, emoji -> folderViewModel.addFolder(name, color, emoji) },
                    onToggleFolder = { id -> folderViewModel.toggleFolder(id) },
                    onFolderClick = { folder -> navController.navigate("folder_detail/${folder.id}") }
                )
            }

            // Tab: Stopwatch
            composable(BottomTab.STOPWATCH.route) {
                StopwatchScreen()
            }

            // Tab: Timer
            composable(BottomTab.TIMER.route) {
                TimerScreen()
            }

            // Alarm Edit (push screen)
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
                        onBack = { navController.popBackStack() },
                        onDelete = { alarmToDelete ->
                            alarmViewModel.deleteAlarm(alarmToDelete)
                            navController.popBackStack()
                        }
                    )
                }
            }

            // Folder Detail (push screen)
            composable(
                route = "folder_detail/{folderId}",
                arguments = listOf(navArgument("folderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getLong("folderId") ?: -1L
                val folder = folders.find { it.id == folderId }

                FolderDetailScreen(
                    folder = folder,
                    alarms = alarms,
                    onBack = { navController.popBackStack() },
                    onToggleFolder = {
                        if (folder != null) folderViewModel.toggleFolder(folder.id)
                    },
                    onAddAlarm = { navController.navigate("alarm_edit/-1") },
                    onAlarmClick = { alarm -> navController.navigate("alarm_edit/${alarm.id}") },
                    onAlarmToggle = { alarm -> alarmViewModel.toggleAlarm(alarm) }
                )
            }
        }
    }
}

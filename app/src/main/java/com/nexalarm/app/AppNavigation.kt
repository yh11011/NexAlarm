package com.nexalarm.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexalarm.app.data.AuthRepository
import com.nexalarm.app.data.SettingsManager
import com.nexalarm.app.data.model.AlarmEntity
import com.nexalarm.app.util.BillingManager
import com.nexalarm.app.util.FeatureFlags
import com.nexalarm.app.ui.screens.*
import com.nexalarm.app.ui.theme.*
import com.nexalarm.app.viewmodel.AlarmViewModel
import com.nexalarm.app.viewmodel.FolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ── Bottom Nav Items (4 tabs) ──
enum class BottomTab(val label: String, val icon: ImageVector, val route: String) {
    ALARM("鬧鐘", Icons.Default.Notifications, "alarm"),
    FOLDERS("資料夾", Icons.Default.Folder, "folders"),
    STOPWATCH("碼錶", Icons.Default.Timer, "stopwatch"),
    TIMER("計時", Icons.Default.HourglassBottom, "timer")
}

// ── All nav items for side drawer ──
// tabIndex: non-null means this item lives inside the pager (tabs route)
private data class DrawerNavItem(val label: String, val icon: ImageVector, val route: String, val tabIndex: Int? = null)

private val allDrawerItems get() = listOf(
    DrawerNavItem(S.home,      Icons.Default.Home,          "home"),
    DrawerNavItem(S.alarm,     Icons.Default.Notifications,  "tabs", 0),
    DrawerNavItem(S.folders,   Icons.Default.Folder,         "tabs", 1),
    DrawerNavItem(S.stopwatch, Icons.Default.Timer,           "tabs", 2),
    DrawerNavItem(S.timer,     Icons.Default.HourglassBottom, "tabs", 3),
    DrawerNavItem(S.settings,  Icons.Default.Settings,       "settings"),
    DrawerNavItem(S.account,   Icons.Default.Person,         "account")
)

@Composable
fun NexAlarmMainContent() {
    val navController = rememberNavController()
    val alarmViewModel: AlarmViewModel = viewModel()
    val folderViewModel: FolderViewModel = viewModel()

    val alarms by alarmViewModel.allAlarms.collectAsState()
    val folders by folderViewModel.allFolders.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val billingManager = remember { BillingManager(context) }

    // ── 帳號狀態（單一來源：SettingsManager；本地 state 僅作 Compose 重組觸發器）──
    val settingsManager = remember { SettingsManager(context) }
    val isFirstLaunch = remember { settingsManager.isFirstLaunch }
    // 用一個整數 tick 作為重組觸發器，避免 username/displayName 各自存一份造成不同步
    var authTick by remember { mutableIntStateOf(0) }
    val authUsername: String? get() = settingsManager.authUsername
    val authDisplayName: String? get() = settingsManager.authDisplayName
    @Suppress("UNUSED_EXPRESSION") authTick // 讓 Compose 追蹤 tick 變化

    // 收集資料夾錯誤訊息（例如超過免費版上限）
    LaunchedEffect(folderViewModel) {
        folderViewModel.errorMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Folder add-dialog state hoisted here so the Scaffold FAB can trigger it
    var showFolderDialog by remember { mutableStateOf(false) }

    // Pre-compute outside the pager so it doesn't re-create on every page scroll
    val alarmCountMap = remember(alarms, folders) {
        folders.associate { folder -> folder.id to alarms.count { it.folderId == folder.id } }
    }

    // Pager hoisted here so both bottom nav and drawer can drive it
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { BottomTab.entries.size })

    // Bottom bar shows on tab pager and on drawer-only main screens（登入頁不顯示）
    val showBottomBar = currentRoute == "tabs" || currentRoute in listOf("home", "settings", "account")

    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }

    CompositionLocalProvider(LocalMenuAction provides openMenu) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = DarkSurface
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "NexAlarm",
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DarkCard)
                    Spacer(Modifier.height(8.dp))

                    allDrawerItems.forEach { item ->
                        val isSelected = if (item.tabIndex != null) {
                            currentRoute == "tabs" && pagerState.currentPage == item.tabIndex
                        } else {
                            currentRoute == item.route
                        }
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(item.label, fontSize = 14.sp) },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.tabIndex != null) {
                                    if (currentRoute != "tabs") {
                                        navController.navigate("tabs") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    scope.launch { pagerState.animateScrollToPage(item.tabIndex) }
                                } else if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = AccentDim,
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = PrimaryBlue,
                                unselectedIconColor = TextSecondary,
                                selectedTextColor = PrimaryBlue,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = DarkBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        PagerBottomBar(
                            tabs = BottomTab.entries,
                            pagerState = pagerState,
                            currentRoute = currentRoute,
                            onTabClick = { index ->
                                if (currentRoute != "tabs") {
                                    navController.navigate("tabs") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                },
                floatingActionButton = {
                    // FAB on alarm (0) and folders (1) only; directly disappears going to stopwatch (2+)
                    if (currentRoute == "tabs") {
                        FloatingActionButton(
                            onClick = {
                                when (pagerState.currentPage) {
                                    0 -> navController.navigate("alarm_edit/-1")
                                    1 -> {
                                        if (FeatureFlags.canCreateFolder(folders.count { !it.isSystem })) {
                                            showFolderDialog = true
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar(S.folderLimitReached) }
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            modifier = Modifier.graphicsLayer {
                                val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                val scale = when {
                                    pos < 0f -> 1f
                                    // Between alarm↔folders: shrink to 70% at midpoint then grow back
                                    pos <= 1f -> 1f - 0.30f * sin(PI * pos.toDouble()).toFloat()
                                    // Past folders toward stopwatch: direct disappear
                                    else -> 0f
                                }
                                scaleX = scale
                                scaleY = scale
                                alpha = scale
                            },
                            containerColor = PrimaryBlue,
                            shape = CircleShape
                        ) {
                            Text("+", fontSize = 24.sp, color = TextPrimary)
                        }
                    }
                }
            ) { padding ->
                // 導航圖：
                // - "login" route → 首次安裝登入頁 / 帳號設定入口
                // - "tabs" route → HorizontalPager（底部 4 個主頁）
                // - 其餘 routes → NavController push 進入的子頁面
                NavHost(
                    navController = navController,
                    startDestination = if (isFirstLaunch) "login?onboarding=true" else "tabs",
                    modifier = Modifier.padding(padding)
                ) {
                    // ── 登入 / 註冊頁 ──
                    // 用明確的路由參數 onboarding=true/false 取代 previousBackStackEntry 判斷，
                    // 避免 APP 被系統回收後重建時判斷錯誤
                    composable(
                        route = "login?onboarding={onboarding}",
                        arguments = listOf(
                            navArgument("onboarding") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) { backStackEntry ->
                        val isOnboarding = backStackEntry.arguments?.getBoolean("onboarding") ?: false
                        LoginScreen(
                            isOnboarding = isOnboarding,
                            onSuccess = { user ->
                                settingsManager.authToken = user.token
                                settingsManager.authUserId = user.id
                                settingsManager.authUsername = user.username ?: user.email
                                settingsManager.authDisplayName = user.displayName
                                settingsManager.isFirstLaunch = false
                                authTick++ // 觸發帳號狀態重組
                                if (isOnboarding) {
                                    navController.navigate("tabs") {
                                        popUpTo("login?onboarding=true") { inclusive = true }
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onSkip = {
                                settingsManager.isFirstLaunch = false
                                navController.navigate("tabs") {
                                    popUpTo("login?onboarding=true") { inclusive = true }
                                }
                            },
                            onBack = if (!isOnboarding) {
                                { navController.popBackStack() }
                            } else null
                        )
                    }
                    // 4 main tabs — HorizontalPager gives connected horizontal slide + swipe
                    composable("tabs") {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true
                        ) { page ->
                            when (page) {
                                0 -> AlarmScreen(
                                    alarms = alarms,
                                    folders = folders,
                                    onAlarmClick = { alarm -> navController.navigate("alarm_edit/${alarm.id}") },
                                    onAlarmToggle = { alarm -> alarmViewModel.toggleAlarm(alarm) }
                                )
                                1 -> FolderManageScreen(
                                    folders = folders,
                                    alarmCountMap = alarmCountMap,
                                    onAddFolder = { name, color, emoji -> folderViewModel.addFolder(name, color, emoji) },
                                    onToggleFolder = { id -> folderViewModel.toggleFolder(id) },
                                    onFolderClick = { folder -> navController.navigate("folder_detail/${folder.id}") },
                                    showAddDialog = showFolderDialog,
                                    onAddDialogDismiss = { showFolderDialog = false },
                                    onAddFolderClick = { showFolderDialog = true }
                                )
                                2 -> StopwatchScreen()
                                3 -> TimerScreen()
                            }
                        }
                    }

                    // Home (drawer-only)
                    composable("home") {
                        HomeScreen(
                            alarms = alarms,
                            onGoToAlarms = {
                                navController.navigate("tabs") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                scope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                    }

                    // Settings (drawer-only)
                    composable("settings") {
                        SettingsScreen()
                    }

                    // Account (drawer-only)
                    composable("account") {
                        AccountScreen(
                            folderUsed = folders.count { !it.isSystem },
                            billingManager = billingManager,
                            onPremiumStatusChanged = { newValue ->
                                FeatureFlags.isPremium = newValue
                                settingsManager.isPremium = newValue
                            },
                            authUsername = authUsername,
                            authDisplayName = authDisplayName,
                            onLoginClick = {
                                navController.navigate("login")
                            },
                            onLogout = {
                                val token = settingsManager.authToken
                                // 先清除本地，再通知後端（背景執行，失敗不影響本地登出）
                                settingsManager.clearAuth()
                                authTick++ // 觸發帳號狀態重組
                                if (token != null) {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        AuthRepository.logout(token)
                                    }
                                }
                            }
                        )
                    }

                    // Alarm Edit (push screen)
                    composable(
                        route = "alarm_edit/{alarmId}?folderId={folderId}",
                        arguments = listOf(
                            navArgument("alarmId") { type = NavType.LongType },
                            navArgument("folderId") { type = NavType.LongType; defaultValue = -1L }
                        )
                    ) { backStackEntry ->
                        val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
                        val folderIdArg = backStackEntry.arguments?.getLong("folderId") ?: -1L
                        val defaultFolderId = if (folderIdArg > 0) folderIdArg else null
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
                                defaultFolderId = defaultFolderId,
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
                            onAddAlarm = { navController.navigate("alarm_edit/-1?folderId=${folderId}") },
                            onAlarmClick = { alarm -> navController.navigate("alarm_edit/${alarm.id}") },
                            onAlarmToggle = { alarm -> alarmViewModel.toggleAlarm(alarm) }
                        )
                    }
                }
            }
        }
    }
}


/**
 * Custom bottom nav bar with a sliding underline indicator that tracks pager position
 * in real-time during swipe gestures — matching the Samsung Clock app reference style.
 */
@Composable
private fun PagerBottomBar(
    tabs: List<BottomTab>,
    pagerState: PagerState,
    currentRoute: String?,
    onTabClick: (Int) -> Unit
) {
    val onTabs = currentRoute == "tabs"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
    ) {
        // Tab items
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val selected = onTabs && pagerState.currentPage == index
                val label = when (tab) {
                    BottomTab.ALARM -> S.alarm
                    BottomTab.FOLDERS -> S.folders
                    BottomTab.STOPWATCH -> S.stopwatch
                    BottomTab.TIMER -> S.timer
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabClick(index) }
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp),
                        tint = if (selected) TextPrimary else TextSecondary.copy(alpha = 0.45f)
                    )
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = if (selected) TextPrimary else TextSecondary.copy(alpha = 0.45f)
                    )
                }
            }
        }

        // Sliding underline indicator — uses layout-phase offset lambda to avoid composition
        // recomposition on every swipe frame (only re-layouts the indicator Box itself)
        if (onTabs) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
            ) {
                val tabWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (maxWidth / tabs.size).toPx()
                }
                val indicatorWidthFraction = 0.45f
                val indicatorWidthPx = tabWidthPx * indicatorWidthFraction
                val indicatorWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) {
                    indicatorWidthPx.toDp()
                }
                val startOffsetPx = (tabWidthPx - indicatorWidthPx) / 2f

                Box(
                    modifier = Modifier
                        .width(indicatorWidthDp)
                        .height(2.dp)
                        // Lambda form: reads pagerState during layout phase, not composition
                        .offset {
                            val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            IntOffset((startOffsetPx + tabWidthPx * pos).roundToInt(), 0)
                        }
                        .background(TextPrimary, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

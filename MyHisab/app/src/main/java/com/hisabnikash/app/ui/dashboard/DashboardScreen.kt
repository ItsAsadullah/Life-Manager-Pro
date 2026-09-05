package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.hisabnikash.app.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import com.hisabnikash.app.auth.AuthManager
import com.hisabnikash.app.sync.CloudSyncManager
import com.hisabnikash.app.sync.SyncStatus
import com.hisabnikash.app.utils.SettingsPreferences
import com.hisabnikash.app.ui.auth.AuthDialog
import android.widget.Toast
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisabnikash.app.data.local.TransactionEntity
import com.hisabnikash.app.data.local.PersonEntity
import com.hisabnikash.app.ui.theme.ThemePreferences
import kotlin.math.min
import kotlin.math.max
import java.text.SimpleDateFormat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hisabnikash.app.utils.NotificationHelper
import com.hisabnikash.app.data.local.BudgetEntity

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    themePreferences: ThemePreferences? = null,
    onAddTransaction: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    
    var showCategoryManagement by remember { mutableStateOf(false) }
    var showQuickEntryManagement by remember { mutableStateOf(false) }
    var selectedPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var showTrashScreen by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddSavingsGoalDialog by remember { mutableStateOf(false) }
    var showAddMarketListDialog by remember { mutableStateOf(false) }
    var showTasksScreen by remember { mutableStateOf(false) }
    var showNotesScreen by remember { mutableStateOf(false) }
    var showRemindersScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showFirstLaunchLoginDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 6 })
    
    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val syncManager = remember { CloudSyncManager.getInstance() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        NotificationHelper.createNotificationChannels(context)

        // Initialize CloudSyncManager and auto-sync if logged in
        syncManager.initPrefs(context)
        if (AuthManager.getInstance().isLoggedIn) {
            val restoreRes = syncManager.smartSyncOnLogin(context)
            val count = restoreRes.getOrDefault(0)
            if (count > 0) {
                Toast.makeText(context, "ক্লাউড থেকে $count টি হিসাব রিস্টোর হয়েছে ✓", Toast.LENGTH_SHORT).show()
            }
        } else {
            // প্রথমবার অ্যাপ ওপেন করার পর ব্যবহারকারীকে লগইন বিষয়ে অবগত করা
            if (!settingsPrefs.hasShownLoginPrompt.value) {
                kotlinx.coroutines.delay(600)
                showFirstLaunchLoginDialog = true
            }
        }
    }

    val categories = remember { mutableStateListOf("দোকান", "লোন", "খাবার", "যাতায়াত", "বেতন", "উপহার") }
    
    val viewModel: TransactionViewModel = viewModel()
    val transactions by viewModel.allTransactions.collectAsState()
    val quickEntries by viewModel.allQuickEntries.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val wallets by viewModel.allWallets.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    var showWalletManagement by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val isOverlayOpen = showSettingsScreen || showTrashScreen || showTasksScreen ||
                    showNotesScreen || showRemindersScreen || showCategoryManagement ||
                    showQuickEntryManagement || showWalletManagement || showNotificationCenter ||
                    showFirstLaunchLoginDialog || (selectedPerson != null)

            if (!isOverlayOpen && pagerState.currentPage < 5) {
                FloatingActionButton(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> showAddDialog = true
                            1 -> showAddPersonDialog = true
                            2 -> showAddBudgetDialog = true
                            3 -> showAddSavingsGoalDialog = true
                            4 -> showAddMarketListDialog = true
                        }
                    },
                    containerColor = if (pagerState.currentPage == 3 || pagerState.currentPage == 4) Color(0xFF30D158) else Color(0xFF0A84FF),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "যোগ করুন", modifier = Modifier.size(32.dp))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LiquidBackgroundGlow()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp)
            ) {
                TopAppBarSection(
                    themePreferences = themePreferences,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    unreadNotificationCount = unreadNotificationCount,
                    onOpenNotifications = { showNotificationCenter = true },
                    onOpenSettings = { showSettingsScreen = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TopNavigationTabs(
                    selectedTab = pagerState.targetPage,
                    pagerState = pagerState,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onTabSelected = { tab ->
                        coroutineScope.launch {
                            if (kotlin.math.abs(pagerState.currentPage - tab) > 1) {
                                pagerState.scrollToPage(tab)
                            } else {
                                pagerState.animateScrollToPage(tab)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page -> 
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val pageAlpha = (1f - kotlin.math.abs(pageOffset) * 0.35f).coerceIn(0f, 1f)
                    val pageTranslationX = pageOffset * with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .graphicsLayer {
                                alpha = pageAlpha
                                translationX = -pageTranslationX
                            }
                    ) {
                        when (page) {
                            0 -> ExpenseDashboard(
                                transactions = transactions,
                                categories = categories,
                                quickEntries = quickEntries,
                                wallets = wallets,
                                onQuickEntryClick = { entry ->
                                    val timeFormat = SimpleDateFormat("hh:mm a", java.util.Locale("en", "US"))
                                    val dateFormat = SimpleDateFormat("dd MMM, yyyy", java.util.Locale("bn", "BD"))
                                    val now = java.util.Calendar.getInstance().time
                                    val defaultWallet = wallets.find { it.isDefault }?.name ?: wallets.firstOrNull()?.name ?: "নগদ ক্যাশ"
                                    viewModel.insert(
                                        com.hisabnikash.app.data.local.TransactionEntity(
                                            title = entry.title,
                                            amount = entry.amount,
                                            isIncome = entry.isIncome,
                                            time = timeFormat.format(now),
                                            date = dateFormat.format(now),
                                            category = entry.category,
                                            paymentMethod = defaultWallet
                                        )
                                    )
                                },
                                onManageCategories = { showCategoryManagement = true },
                                onManageQuickEntries = { showQuickEntryManagement = true },
                                onEditTransaction = { tx -> transactionToEdit = tx },
                                onDeleteTransaction = { tx -> viewModel.delete(tx) },
                                viewModel = viewModel
                            )
                            1 -> LendBorrowScreen(
                                viewModel = viewModel,
                                onPersonClick = { person ->
                                    selectedPerson = person
                                },
                                onTrashClick = { showTrashScreen = true },
                                showAddPersonExternally = showAddPersonDialog,
                                onAddPersonDismissed = { showAddPersonDialog = false }
                            )
                            2 -> BudgetScreen(
                                viewModel = viewModel,
                                categories = categories,
                                onManageCategories = { showCategoryManagement = true },
                                showAddExternally = showAddBudgetDialog,
                                onAddDismissed = { showAddBudgetDialog = false }
                            )
                            3 -> SavingsScreen(
                                viewModel = viewModel,
                                showAddGoalExternally = showAddSavingsGoalDialog,
                                onAddGoalDismissed = { showAddSavingsGoalDialog = false }
                            )
                            4 -> com.hisabnikash.app.ui.market.MarketMemoScreen(
                                viewModel = viewModel,
                                showAddExternally = showAddMarketListDialog,
                                onAddDismissed = { showAddMarketListDialog = false }
                            )
                            5 -> MoreToolsScreen(
                                onNavigateToTasks = { showTasksScreen = true },
                                onNavigateToNotes = { showNotesScreen = true },
                                onNavigateToReminders = { showRemindersScreen = true },
                                onNavigateToNotifications = { showNotificationCenter = true }
                            )
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddTransactionDialog(
                    categories = categories,
                    wallets = wallets,
                    onManageCategories = { showCategoryManagement = true },
                    onManageWallets = { showWalletManagement = true },
                    onDismiss = { showAddDialog = false },
                    onSave = { isIncome, amountStr, note, date, time, category, isRecurring, recurringPeriod, paymentMethod ->
                        val amount = amountStr.toEnglishDouble()
                        if (amount > 0) {
                            val title = note.ifBlank { category.ifBlank { if (isIncome) "আয়" else "ব্যয়" } }
                            viewModel.insert(
                                TransactionEntity(
                                    title = title,
                                    amount = amount,
                                    isIncome = isIncome,
                                    time = time,
                                    date = date,
                                    category = category,
                                    isRecurring = isRecurring,
                                    recurringPeriod = recurringPeriod,
                                    paymentMethod = paymentMethod.ifBlank { wallets.find { it.isDefault }?.name ?: "নগদ ক্যাশ" }
                                )
                            )
                            checkBudgetWarning(context, category, amount, date, isIncome, transactions, budgets, null)
                        }
                        showAddDialog = false
                    },
                    onSaveQuickEntry = { isIncome, amountStr, note, category ->
                        val amount = amountStr.toEnglishDouble()
                        if (amount > 0) {
                            val title = note.ifBlank { category.ifBlank { if (isIncome) "আয়" else "ব্যয়" } }
                            viewModel.insertQuickEntry(com.hisabnikash.app.data.local.QuickEntryEntity(title = title, amount = amount, isIncome = isIncome, category = category))
                        }
                        showAddDialog = false
                    }
                )
            }

            if (transactionToEdit != null) {
                EditTransactionDialog(
                    transaction = transactionToEdit!!,
                    categories = categories,
                    wallets = wallets,
                    onManageCategories = { showCategoryManagement = true },
                    onManageWallets = { showWalletManagement = true },
                    onDismiss = { transactionToEdit = null },
                    onUpdate = { updatedTx ->
                        viewModel.update(updatedTx)
                        checkBudgetWarning(context, updatedTx.category, updatedTx.amount, updatedTx.date, updatedTx.isIncome, transactions, budgets, updatedTx.id)
                        transactionToEdit = null
                    }
                )
            }

            // Quick Entry Management Screen
            if (showQuickEntryManagement) {
                QuickEntryManageScreen(
                    viewModel = viewModel,
                    categories = categories,
                    onDismiss = { showQuickEntryManagement = false }
                )
            }
            
            if (showCategoryManagement) {
                CategoryManagementDialog(
                    categories = categories,
                    onDismiss = { showCategoryManagement = false },
                    onAddCategory = { categories.add(it) },
                    onUpdateCategory = { old, new ->
                        val index = categories.indexOf(old)
                        if (index != -1) categories[index] = new
                        
                        // Update existing transactions
                        transactions.filter { it.category == old }.forEach { tx ->
                            viewModel.update(tx.copy(category = new))
                        }
                    },
                    onDeleteCategory = { cat ->
                        categories.remove(cat)
                        if (!categories.contains("অন্যান্য")) categories.add("অন্যান্য")
                        
                        // Move existing to "অন্যান্য"
                        transactions.filter { it.category == cat }.forEach { tx ->
                            viewModel.update(tx.copy(category = "অন্যান্য"))
                        }
                    },
                    onUpdateList = { newList ->
                        categories.clear()
                        categories.addAll(newList)
                    }
                )
            }
            
            if (selectedPerson != null) {
                PersonDetailScreen(
                    person = selectedPerson!!,
                    viewModel = viewModel,
                    onBack = { selectedPerson = null }
                )
            }
            
            if (showTrashScreen) {
                TrashScreen(
                    viewModel = viewModel,
                    onBack = { showTrashScreen = false }
                )
            }
            
            if (showTasksScreen) {
                TasksScreen(
                    viewModel = viewModel,
                    onBack = { showTasksScreen = false }
                )
            }
            
            if (showNotesScreen) {
                com.hisabnikash.app.ui.notes.NotesScreen(
                    viewModel = viewModel,
                    onBack = { showNotesScreen = false }
                )
            }

            if (showRemindersScreen) {
                com.hisabnikash.app.ui.reminders.RemindersScreen(
                    viewModel = viewModel,
                    onBack = { showRemindersScreen = false }
                )
            }

            if (showSettingsScreen) {
                com.hisabnikash.app.ui.settings.SettingsScreen(
                    viewModel = viewModel,
                    themePreferences = themePreferences,
                    onBack = { showSettingsScreen = false },
                    onNavigateToCategories = {
                        showSettingsScreen = false
                        showCategoryManagement = true
                    },
                    onNavigateToBudget = {
                        showSettingsScreen = false
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onNavigateToDebts = {
                        showSettingsScreen = false
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
            }

            if (showWalletManagement) {
                com.hisabnikash.app.ui.wallet.WalletManagementScreen(
                    viewModel = viewModel,
                    onBack = { showWalletManagement = false }
                )
            }

            if (showNotificationCenter) {
                com.hisabnikash.app.ui.notifications.NotificationCenterScreen(
                    viewModel = viewModel,
                    onBack = { showNotificationCenter = false },
                    onNavigateToReminders = {
                        showNotificationCenter = false
                        showRemindersScreen = true
                    },
                    onNavigateToBudget = {
                        showNotificationCenter = false
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onNavigateToDashboard = {
                        showNotificationCenter = false
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    }
                )
            }

            // প্রথমবার অ্যাপ খোলার পর লগইন নোটিস / প্রম্পট ডায়ালগ
            if (showFirstLaunchLoginDialog) {
                AuthDialog(
                    onDismiss = {
                        showFirstLaunchLoginDialog = false
                        settingsPrefs.setHasShownLoginPrompt(true)
                    },
                    onAuthSuccess = {
                        showFirstLaunchLoginDialog = false
                        settingsPrefs.setHasShownLoginPrompt(true)
                        coroutineScope.launch {
                            val res = syncManager.sync(context)
                            if (res.isSuccess) {
                                Toast.makeText(context, "লগইন সফল! ক্লাউড ডেটা সিঙ্ক সম্পন্ন হয়েছে ✓", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TopAppBarSection(
    themePreferences: ThemePreferences?,
    modifier: Modifier = Modifier,
    unreadNotificationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val isDarkTheme = themePreferences?.isDarkTheme?.collectAsState()?.value ?: true
    val iconTint = if (isDarkTheme) Color.White else Color.Black

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Text("হিসাব নিকাশ", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = iconTint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // নোটিফিকেশন বেল উইথ ব্যাজ
            Box(
                modifier = Modifier
                    .clickable { onOpenNotifications() }
                    .padding(4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notification",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                if (unreadNotificationCount > 0) {
                    val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
                    val countStr = if (unreadNotificationCount > 9) "৯+" else unreadNotificationCount.toString().map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
                    Box(
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-4).dp)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countStr,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 10.sp
                            ),
                            modifier = Modifier.offset(y = (-0.8).dp)
                        )
                    }
                }
            }
            Icon(
                Icons.Default.DarkMode,
                contentDescription = "Dark Mode",
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { themePreferences?.toggleTheme() }
            )
            val authUser by AuthManager.getInstance().currentUser.collectAsState()
            if (authUser != null) {
                val syncStatus by CloudSyncManager.getInstance().syncStatus.collectAsState()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    if (syncStatus == SyncStatus.SYNCING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0A84FF)
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "Cloud Sync Active",
                            tint = Color(0xFF30D158),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onOpenSettings() }
            )
        }
    }
}

@Composable
fun TopNavigationTabs(
    selectedTab: Int,
    modifier: Modifier = Modifier,
    pagerState: androidx.compose.foundation.pager.PagerState? = null,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        com.hisabnikash.app.ui.components.TabItem(Icons.AutoMirrored.Filled.CompareArrows, "হিসাব"),
        com.hisabnikash.app.ui.components.TabItem(Icons.Default.SyncAlt, "পাবো/দিবো"),
        com.hisabnikash.app.ui.components.TabItem(Icons.Default.Timeline, "বাজেট"),
        com.hisabnikash.app.ui.components.TabItem(Icons.Default.Savings, "সঞ্চয়"),
        com.hisabnikash.app.ui.components.TabItem(Icons.Default.ShoppingCart, "বাজার লিস্ট"),
        com.hisabnikash.app.ui.components.TabItem(Icons.Default.Apps, "মোর টুলস")
    )

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabSpacing = 8.dp
        val availableWidth = maxWidth - (tabSpacing * (tabs.size - 1))
        val tabWidth = availableWidth / tabs.size

        com.hisabnikash.app.ui.components.LiquidGlassTabBar(
            tabs = tabs,
            selectedTab = selectedTab,
            pagerState = pagerState,
            tabWidth = tabWidth,
            tabSpacing = tabSpacing,
            modifier = Modifier,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
fun LiquidBackgroundGlow() {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val alphaMulti = if (isDark) 1f else 0.3f
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.size(400.dp).align(Alignment.TopStart).offset(x = (-50).dp, y = (-50).dp).background(Brush.radialGradient(listOf(Color(0xFF007AFF).copy(alpha = 0.45f * alphaMulti), Color.Transparent))))
        Box(modifier = Modifier.size(350.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = 150.dp).background(Brush.radialGradient(listOf(Color(0xFF5856D6).copy(alpha = 0.4f * alphaMulti), Color.Transparent))))
        Box(modifier = Modifier.size(450.dp).align(Alignment.BottomCenter).offset(y = 100.dp).background(Brush.radialGradient(listOf(Color(0xFFFF2D55).copy(alpha = 0.3f * alphaMulti), Color.Transparent))))
    }
}

@Composable
fun EmptyStateContent(icon: ImageVector, title: String, subtitle: String) {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryColor = if (isDark) TextWhitePrimary else Color(0xFF1C1C1E)
    val secondaryColor = if (isDark) TextWhiteSecondary else Color(0xFF8E8E93)

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = secondaryColor.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = primaryColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, fontSize = 14.sp, color = secondaryColor)
    }
}

fun Double.toBanglaString(): String {
    val format = java.text.DecimalFormat("#,##0.##")
    val enStr = format.format(this)
    val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return enStr.map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
}

fun Long.toBanglaString(): String {
    val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return this.toString().map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
}

fun String.toEnglishDigits(): String {
    val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    var result = this
    for (i in 0..9) {
        result = result.replace(banglaDigits[i], englishDigits[i])
    }
    return result.replace('।', '.').replace('٫', '.')
}

fun String.toEnglishDouble(): Double {
    val cleaned = this.toEnglishDigits().replace(",", "").trim()
    return cleaned.toDoubleOrNull() ?: 0.0
}


fun checkBudgetWarning(
    context: android.content.Context,
    category: String,
    amount: Double,
    dateStr: String,
    isIncome: Boolean,
    transactions: List<com.hisabnikash.app.data.local.TransactionEntity>,
    budgets: List<com.hisabnikash.app.data.local.BudgetEntity>,
    txIdToExclude: String?
) {
    if (isIncome) return
    try {
        val dateFormat = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale("bn", "BD"))
        val dateObj = dateFormat.parse(dateStr) ?: return
        val cal = java.util.Calendar.getInstance().apply { time = dateObj }
        val monthYearKey = String.format(java.util.Locale.US, "%04d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)

        val budget = budgets.find { it.category == category && it.monthYear == monthYearKey }
        if (budget != null) {
            val displayMonthShort = java.text.SimpleDateFormat("MMM", java.util.Locale("bn", "BD")).format(dateObj)
            val displayYearBangla = cal.get(java.util.Calendar.YEAR).toBanglaString()
            
            val monthTransactions = transactions.filter { tx ->
                displayMonthShort in tx.date && displayYearBangla in tx.date && tx.category == category && !tx.isIncome && tx.id != txIdToExclude
            }
            
            val currentTotal = monthTransactions.sumOf { it.amount }
            val newTotal = currentTotal + amount
            
            if (newTotal > budget.monthlyLimit) {
                val overAmount = newTotal - budget.monthlyLimit
                val percentage = if (budget.monthlyLimit > 0) ((overAmount / budget.monthlyLimit) * 100).toInt() else 0
                val toastText = "আপনার '${category}' বাজেট ${budget.monthlyLimit.toBanglaString()} টাকা, আপনি খরচ করেছেন ${newTotal.toBanglaString()} টাকা যা আপনার বাজেটের ${percentage.toDouble().toBanglaString()}% বেশি। ⚠️"
                android.widget.Toast.makeText(context, toastText, android.widget.Toast.LENGTH_LONG).show()
                com.hisabnikash.app.utils.NotificationHelper.showBudgetWarningNotification(context, category, budget.monthlyLimit, newTotal)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

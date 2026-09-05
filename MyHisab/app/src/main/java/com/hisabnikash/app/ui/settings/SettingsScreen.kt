package com.hisabnikash.app.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.hisabnikash.app.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import coil.compose.AsyncImage
import com.hisabnikash.app.auth.AuthManager
import com.hisabnikash.app.sync.CloudSyncManager
import com.hisabnikash.app.sync.SyncStatus
import com.hisabnikash.app.ui.auth.AuthDialog
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.TransactionViewModel
import com.hisabnikash.app.ui.dashboard.toEnglishDigits
import com.hisabnikash.app.ui.market.IOSInputField
import com.hisabnikash.app.ui.theme.ThemePreferences
import com.hisabnikash.app.ui.wallet.WalletManagementScreen
import com.hisabnikash.app.utils.BackupHelper
import com.hisabnikash.app.utils.SettingsPreferences
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    themePreferences: ThemePreferences? = null,
    onBack: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsPrefs = remember { SettingsPreferences(context) }

    val userName by settingsPrefs.userName.collectAsState()
    val userPhoneOrEmail by settingsPrefs.userPhoneOrEmail.collectAsState()
    val userCurrency by settingsPrefs.userCurrency.collectAsState()
    val hideBalance by settingsPrefs.hideBalance.collectAsState()
    val glassIntensity by settingsPrefs.glassIntensity.collectAsState()
    val useBanglaDigits by settingsPrefs.useBanglaDigits.collectAsState()
    val budgetWarning by settingsPrefs.budgetWarningEnabled.collectAsState()
    val dailyReminder by settingsPrefs.dailyReminderEnabled.collectAsState()
    val appLockEnabled by settingsPrefs.appLockEnabled.collectAsState()
    val appLockPin by settingsPrefs.appLockPin.collectAsState()
    val biometricEnabled by settingsPrefs.biometricEnabled.collectAsState()
    val isHardwareBiometric = remember { com.hisabnikash.app.utils.BiometricHelper.isHardwareAvailable(context) }
    val lastBackupTime by settingsPrefs.lastBackupTime.collectAsState()

    val isDarkTheme = themePreferences?.isDarkTheme?.collectAsState()?.value ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)

    // ডায়ালগ কন্ট্রোল ফ্ল্যাগসমূহ
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var isBackingUp by remember { mutableStateOf(false) }
    var backupResultFile by remember { mutableStateOf<File?>(null) }
    var backupRecordCount by remember { mutableStateOf(0) }
    var showBackupResultDialog by remember { mutableStateOf(false) }

    val wallets by viewModel.allWallets.collectAsState()
    var showWalletScreen by remember { mutableStateOf(false) }

    val authManager = remember { AuthManager.getInstance() }
    val syncManager = remember { CloudSyncManager.getInstance() }
    val currentUser by authManager.currentUser.collectAsState()
    val syncStatus by syncManager.syncStatus.collectAsState()
    val lastSyncTime by syncManager.lastSyncTime.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }
    var showCloudRestoreDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var wipeCloudDataAlso by remember { mutableStateOf(false) }
    var isWipingData by remember { mutableStateOf(false) }

    var storageInfo by remember { mutableStateOf(Pair("হিসাব করা হচ্ছে...", "হিসাব করা হচ্ছে...")) }
    LaunchedEffect(Unit) {
        syncManager.initPrefs(context)
        storageInfo = BackupHelper.getStorageInfo(context)
    }

    if (showWalletScreen) {
        WalletManagementScreen(
            viewModel = viewModel,
            onBack = { showWalletScreen = false }
        )
        return
    }

    // ফাইল পিকার (ব্যাকআপ রিস্টোর করার জন্য)
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: ""
                    if (jsonStr.isNotBlank()) {
                        val count = BackupHelper.restoreDatabaseFromJson(context, jsonStr)
                        storageInfo = BackupHelper.getStorageInfo(context)
                        Toast.makeText(context, "সফলভাবে $count টি রেকর্ড রিস্টোর করা হয়েছে!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে: সঠিক ব্যাকআপ ফাইল নির্বাচন করুন", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // ১. শীর্ষ নেভিগেশন বার
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("সেটিংস (Settings)", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ২. স্ক্রোলযোগ্য সেটিংস তালিকা (Apple Inset Grouped Table - 120 FPS Smooth)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ==========================================
                // ১. প্রোফাইল ও ক্লাউড অ্যাকাউন্ট কার্ড
                // ==========================================
                IOSSettingsGroup(
                    modifier = Modifier.fillMaxWidth(),
                    isDark = isDarkTheme
                ) {
                    if (currentUser != null) {
                        // লগইন করা অবস্থা
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (currentUser?.photoUrl != null) {
                                        AsyncImage(
                                            model = currentUser?.photoUrl,
                                            contentDescription = "Profile",
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color(0xFF30D158), CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (currentUser?.displayName ?: userName).take(1).uppercase(),
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = currentUser?.displayName ?: userName,
                                            fontSize = 16.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentUser?.email ?: "",
                                            fontSize = 12.sp,
                                            color = subtextColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF30D158).copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color(0xFF30D158),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "ক্লাউড সিঙ্ক সক্রিয়",
                                                fontSize = 10.sp,
                                                color = Color(0xFF30D158),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { showLogoutConfirmDialog = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "লগআউট",
                                        tint = Color(0xFFFF453A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = subtextColor.copy(alpha = 0.12f), thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // সিঙ্ক স্ট্যাটাস ও বাটন
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (syncStatus == SyncStatus.SYNCING) "ক্লাউডে সিঙ্ক হচ্ছে..." else formatSyncTime(lastSyncTime),
                                        fontSize = 12.sp,
                                        color = if (syncStatus == SyncStatus.SYNCING) Color(0xFF0A84FF) else subtextColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // ক্লাউড রিস্টোর বাটন
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0A84FF).copy(alpha = 0.12f))
                                            .clickable { showCloudRestoreDialog = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "রিস্টোর ⬇️",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0A84FF)
                                        )
                                    }

                                    // এখনই সিঙ্ক বাটন
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF30D158))
                                            .clickable(enabled = syncStatus != SyncStatus.SYNCING) {
                                                coroutineScope.launch {
                                                    val res = syncManager.sync(context)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "ক্লাউডে সিঙ্ক সম্পন্ন হয়েছে ✓", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "সিঙ্ক ত্রুটি", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (syncStatus == SyncStatus.SYNCING) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "সিঙ্ক করুন",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // লগইন না করা অবস্থা
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFFF9500), Color(0xFFFF3B30))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Cloud Off",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "অনলাইন ক্লাউড সিঙ্ক",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ডাটা সুরক্ষায় লগইন করুন",
                                        fontSize = 12.5.sp,
                                        color = subtextColor
                                    )
                                }
                            }

                            Button(
                                onClick = { showAuthDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("লগইন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // ২. হিসাব ও ফাইন্যান্স সেটিংস
                // ==========================================
                Column {
                    SettingsSectionHeader("হিসাব ও ফাইন্যান্স")
                    Spacer(modifier = Modifier.height(6.dp))
                    IOSSettingsGroup(isDark = isDarkTheme) {
                        SettingsRowItem(
                            title = "অ্যাকাউন্ট ও ওয়ালেট",
                            subtitle = if (wallets.isNotEmpty()) "${wallets.size} টি অ্যাকাউন্ট • ডিফল্ট: ${wallets.find { it.isDefault }?.name ?: "নগদ ক্যাশ"}" else "১ টি অ্যাকাউন্ট • ডিফল্ট: নগদ ক্যাশ",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBg = Color(0xFF0A84FF),
                            trailingText = "${if (wallets.isEmpty()) 1 else wallets.size} টি",
                            onClick = { showWalletScreen = true }
                        )
                    }
                }

                // ==========================================
                // ৩. অ্যাপ সেটিংস (থিম, ভাষা, নোটিফিকেশন, সিকিউরিটি)
                // ==========================================
                Column {
                    SettingsSectionHeader("অ্যাপ সেটিংস")
                    Spacer(modifier = Modifier.height(6.dp))
                    IOSSettingsGroup(isDark = isDarkTheme) {
                        // ডার্ক মোড সুইচ
                        SettingsSwitchItem(
                            title = "ডার্ক মোড (Dark Theme)",
                            subtitle = if (isDarkTheme) "ডার্ক মোড সক্রিয়" else "লাইট মোড সক্রিয়",
                            icon = Icons.Default.BrightnessMedium,
                            iconBg = Color(0xFF5856D6),
                            isChecked = isDarkTheme,
                            onCheckedChange = { themePreferences?.toggleTheme() }
                        )
                        SettingsDivider()
                        // লিকুইড গ্লাস ইফেক্ট ইনটেনসিটি
                        SettingsRowItem(
                            title = "লিকুইড গ্লাস ইফেক্ট",
                            subtitle = "ব্লার ও গ্লাস তীব্রতা নির্ধারণ",
                            icon = Icons.Default.Palette,
                            iconBg = Color(0xFFFF2D55),
                            trailingText = glassIntensity,
                            onClick = { showAppearanceDialog = true }
                        )
                        SettingsDivider()
                        // ভাষা ও ডিজিট ফরম্যাট
                        SettingsRowItem(
                            title = "মুদ্রা ও সংখ্যা ফরম্যাট",
                            subtitle = if (useBanglaDigits) "বাংলা সংখ্যা (১, ২, ৩...)" else "ইংরেজি সংখ্যা (1, 2, 3...)",
                            icon = Icons.Default.Language,
                            iconBg = Color(0xFF007AFF),
                            trailingText = userCurrency,
                            onClick = { showLanguageDialog = true }
                        )
                        SettingsDivider()
                        // ব্যালেন্স গোপন রাখা (Hide Balance)
                        SettingsSwitchItem(
                            title = "মোট ব্যালেন্স গোপন রাখুন",
                            subtitle = "ড্যাশবোর্ডে ব্যালেন্স স্টার (••••••) দেখাবে",
                            icon = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            iconBg = Color(0xFFFF9500),
                            isChecked = hideBalance,
                            onCheckedChange = { settingsPrefs.setHideBalance(it) }
                        )
                        SettingsDivider()
                        // বাজেট ওভারস্পেন্ডিং নোটিফিকেশন
                        SettingsSwitchItem(
                            title = "বাজেট সতর্কতা নোটিফিকেশন",
                            subtitle = "বাজেট সীমা ছাড়িয়ে গেলে সতর্কবার্তা",
                            icon = Icons.Default.Notifications,
                            iconBg = Color(0xFFFF3B30),
                            isChecked = budgetWarning,
                            onCheckedChange = { settingsPrefs.setBudgetWarning(it) }
                        )
                        SettingsDivider()
                        // দৈনিক নোটিফিকেশন ও রিমাইন্ডার
                        SettingsSwitchItem(
                            title = "দৈনিক নোটিফিকেশন ও আপডেট",
                            subtitle = "সকাল, দুপুর ও রাতে খরচের হিসাব রাখার নোটিফিকেশন",
                            icon = Icons.Default.Notifications,
                            iconBg = Color(0xFF0A84FF),
                            isChecked = dailyReminder,
                            onCheckedChange = { settingsPrefs.setDailyReminder(it) }
                        )
                        SettingsDivider()
                        // অ্যাপ লক / পিন সিকিউরিটি সুইচ
                        SettingsSwitchItem(
                            title = "অ্যাপ লক সিকিউরিটি",
                            subtitle = if (appLockEnabled) "লক সক্রিয় (PIN ও বায়োমেট্রিক)" else "লক নিষ্ক্রিয়",
                            icon = Icons.Default.Lock,
                            iconBg = Color(0xFF34C759),
                            isChecked = appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPinDialog = true
                                } else {
                                    settingsPrefs.setAppLock(false)
                                    Toast.makeText(context, "অ্যাপ লক বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        if (appLockEnabled) {
                            if (isHardwareBiometric) {
                                SettingsDivider()
                                SettingsSwitchItem(
                                    title = "ফিঙ্গারপ্রিন্ট আনলক",
                                    subtitle = "ফিঙ্গারপ্রিন্ট সেন্সর দিয়ে দ্রুত আনলক করুন",
                                    icon = Icons.Default.Fingerprint,
                                    iconBg = Color(0xFF0A84FF),
                                    isChecked = biometricEnabled,
                                    onCheckedChange = { settingsPrefs.setBiometricEnabled(it) }
                                )
                            }

                            SettingsDivider()
                            SettingsRowItem(
                                title = "PIN পরিবর্তন করুন",
                                subtitle = "আপনার ৪-ডিজিট সিকিউরিটি পিন পরিবর্তন করুন",
                                icon = Icons.Default.Password,
                                iconBg = Color(0xFFFF9500),
                                onClick = { showPinDialog = true }
                            )
                        }
                    }
                }

                // ==========================================
                // ৪. ডেটা ও ব্যাকআপ (Backup, Restore, CSV)
                // ==========================================
                Column {
                    SettingsSectionHeader("ডেটা ও ব্যাকআপ")
                    Spacer(modifier = Modifier.height(6.dp))
                    IOSSettingsGroup(isDark = isDarkTheme) {
                        // ক্লাউড ব্যাকআপ ও সিঙ্ক (Firebase)
                        SettingsRowItem(
                            title = "ক্লাউড ব্যাকআপ ও সিঙ্ক (Firebase)",
                            subtitle = if (currentUser != null) "অ্যাকাউন্ট: ${currentUser?.email ?: currentUser?.displayName} • সিঙ্ক সক্রিয়" else "লগইন করে ক্লাউড ব্যাকআপ ও ডেটা সিঙ্ক করুন",
                            icon = Icons.Default.CloudSync,
                            iconBg = Color(0xFF30D158),
                            trailingText = if (currentUser != null) "সিঙ্ক" else "লগইন",
                            onClick = {
                                if (currentUser != null) {
                                    coroutineScope.launch {
                                        val res = syncManager.sync(context)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "ক্লাউড সিঙ্ক সম্পন্ন হয়েছে ✓", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "সিঙ্ক ত্রুটি", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    showAuthDialog = true
                                }
                            }
                        )

                        SettingsDivider()

                        // ১. ব্যাকআপ নাও
                        SettingsRowItem(
                            title = "সম্পূর্ণ ডেটা ব্যাকআপ নিন",
                            subtitle = "লাস্ট ব্যাকআপ: $lastBackupTime",
                            icon = Icons.Default.CloudUpload,
                            iconBg = Color(0xFF0A84FF),
                            trailingContent = {
                                if (isBackingUp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0A84FF))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0A84FF).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Backup Now", fontSize = 11.sp, color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = {
                                if (!isBackingUp) {
                                    isBackingUp = true
                                    coroutineScope.launch {
                                        try {
                                            val (file, count) = BackupHelper.createFullBackupJson(context)
                                            backupResultFile = file
                                            backupRecordCount = count
                                            val nowStr = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD")).format(Date())
                                            settingsPrefs.setLastBackupTime(nowStr)
                                            showBackupResultDialog = true
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "ব্যাকআপ তৈরিতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isBackingUp = false
                                        }
                                    }
                                }
                            }
                        )

                        SettingsDivider()

                        // ২. ব্যাকআপ রিস্টোর
                        SettingsRowItem(
                            title = "ব্যাকআপ রিস্টোর করুন",
                            subtitle = "পূর্বের সেভ করা JSON ফাইল থেকে ডেটা ফেরান",
                            icon = Icons.Default.CloudDownload,
                            iconBg = Color(0xFF30D158),
                            onClick = {
                                restoreFilePicker.launch("*/*")
                            }
                        )

                        SettingsDivider()

                        // ৩. এক্সেল / CSV এক্সপোর্ট
                        SettingsRowItem(
                            title = "এক্সেল / CSV এক্সপোর্ট",
                            subtitle = "সমস্ত লেনদেনের বিবরণ স্প্রেডশীটে শেয়ার করুন",
                            icon = Icons.Default.Description,
                            iconBg = Color(0xFF007AFF),
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val (csvFile, count) = BackupHelper.exportTransactionsToCsv(context)
                                        BackupHelper.shareFile(context, csvFile, "text/csv", "লেনদেনের CSV ফাইল শেয়ার করুন")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "CSV এক্সপোর্টে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        SettingsDivider()

                        // ৪. স্টোরেজ তথ্য ও ক্লিয়ার ক্যাশ
                        SettingsRowItem(
                            title = "স্টোরেজ ও ক্যাশ মেমরি",
                            subtitle = "ডাটাবেস: ${storageInfo.first} • ক্যাশ: ${storageInfo.second}",
                            icon = Icons.Default.Storage,
                            iconBg = Color(0xFF8E8E93),
                            trailingText = "ক্লিয়ার",
                            onClick = {
                                coroutineScope.launch {
                                    BackupHelper.clearAppCache(context)
                                    storageInfo = BackupHelper.getStorageInfo(context)
                                    Toast.makeText(context, "ক্যাশ মেমরি পরিষ্কার করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        SettingsDivider()

                        // ৫. সমস্ত ডেটা মুছে ফেলুন (Reset)
                        SettingsRowItem(
                            title = "সমস্ত ডেটা রিসেট করুন",
                            subtitle = "ফ্যাক্টরি রিসেট (সতর্কতা সহকারে ব্যবহার করুন)",
                            icon = Icons.Default.DeleteSweep,
                            iconBg = Color(0xFFFF3B30),
                            titleColor = Color(0xFFFF3B30),
                            onClick = { showResetDialog = true }
                        )
                    }
                }

                // ==========================================
                // ৫. অন্যান্য ও অ্যাপ সম্পর্কিত তথ্য
                // ==========================================
                Column {
                    SettingsSectionHeader("অ্যাপ সম্পর্কিত তথ্য")
                    Spacer(modifier = Modifier.height(6.dp))
                    IOSSettingsGroup(isDark = isDarkTheme) {
                        SettingsRowItem(
                            title = "হিসাব নিকাশ সম্পর্কে (About)",
                            subtitle = "ভার্সন v1.0.0 Pro • TechHat",
                            icon = Icons.Default.Info,
                            iconBg = Color(0xFF0A84FF),
                            onClick = { showAboutDialog = true }
                        )
                        SettingsDivider()
                        SettingsRowItem(
                            title = "প্রাইভেসি পলিসি ও নিরাপত্তা",
                            subtitle = "আপনার ডেটা ১০০% অফলাইন ও আপনার ডিভাইসে সুরক্ষিত",
                            icon = Icons.Default.PrivacyTip,
                            iconBg = Color(0xFF34C759),
                            onClick = { showAboutDialog = true }
                        )
                        SettingsDivider()
                        SettingsRowItem(
                            title = "অ্যাপ রেটিং ও সাপোর্ট",
                            subtitle = "ডেভেলপার টিমের সাথে যোগাযোগ",
                            icon = Icons.Default.Star,
                            iconBg = Color(0xFFFFCC00),
                            onClick = {
                                Toast.makeText(context, "Life Manager Pro ব্যবহার করার জন্য ধন্যবাদ!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    // ==========================================
    // ডায়ালগসমূহ
    // ==========================================

    // ১. প্রোফাইল এডিট ডায়ালগ
    if (showProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentPhone = userPhoneOrEmail,
            currentCurrency = userCurrency,
            isDark = isDarkTheme,
            onDismiss = { showProfileDialog = false },
            onSave = { newName, newPhone, newCurrency ->
                settingsPrefs.updateProfile(newName, newPhone, newCurrency)
                showProfileDialog = false
            }
        )
    }

    // ২. ব্যাকআপ সম্পন্ন রেজাল্ট ডায়ালগ
    if (showBackupResultDialog && backupResultFile != null) {
        Dialog(onDismissRequest = { showBackupResultDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF30D158).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ব্যাকআপ সফল হয়েছে! 🎉", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "মোট $backupRecordCount টি রেকর্ড সুরক্ষিতভাবে সেভ হয়েছে। আপনি এটি ক্লাউড ড্রাইভ (Google Drive), হোয়াটসঅ্যাপ বা ডিভাইসে সংরক্ষণ করতে পারেন।",
                        fontSize = 13.sp,
                        color = subtextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showBackupResultDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("ঠিক আছে", color = textColor)
                        }
                        Button(
                            onClick = {
                                BackupHelper.shareFile(context, backupResultFile!!, "application/json", "ব্যাকআপ ফাইল সংরক্ষণ বা শেয়ার করুন")
                                showBackupResultDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার / সেভ", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ৩. অ্যাপিয়ারেন্স ও গ্লাস ইনটেনসিটি ডায়ালগ
    if (showAppearanceDialog) {
        Dialog(onDismissRequest = { showAppearanceDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("লিকুইড গ্লাস তীব্রতা (Glass Effect)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("Off", "Low", "Medium", "High").forEach { level ->
                        val isSelected = glassIntensity == level
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    settingsPrefs.setGlassIntensity(level)
                                    showAppearanceDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(level, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textColor)
                            if (isSelected) {
                                Text("✓", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ৪. মুদ্রা ও সংখ্যা ফরম্যাট ডায়ালগ
    if (showLanguageDialog) {
        Dialog(onDismissRequest = { showLanguageDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("মুদ্রা ও ডিজিট ফরম্যাট", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("মুদ্রা প্রতীক:", fontSize = 13.sp, color = subtextColor, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("৳ BDT", "$ USD", "€ EUR", "₹ INR").forEach { cur ->
                            val isSel = userCurrency == cur
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF0A84FF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable { settingsPrefs.updateProfile(userName, userPhoneOrEmail, cur) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cur, fontSize = 12.sp, color = if (isSel) Color.White else textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("সংখ্যা রূপান্তর:", fontSize = 13.sp, color = subtextColor, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { settingsPrefs.setUseBanglaDigits(!useBanglaDigits) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বাংলা সংখ্যা প্রদর্শন (১, ২, ৩...)", fontSize = 14.sp, color = textColor)
                        Switch(
                            checked = useBanglaDigits,
                            onCheckedChange = { settingsPrefs.setUseBanglaDigits(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0A84FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showLanguageDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সম্পন্ন", color = Color.White)
                    }
                }
            }
        }
    }

    // ৫. সিকিউরিটি পিন ডায়ালগ
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showPinDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("অ্যাপ লক সিকিউরিটি PIN", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("৪-সংখ্যার গোপনীয় পিন দিয়ে আপনার হিসাব সুরক্ষিত রাখুন।", fontSize = 13.sp, color = subtextColor)
                    Spacer(modifier = Modifier.height(14.dp))
                    IOSInputField(
                        title = "৪-ডিজিট পিন",
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it.toEnglishDigits() },
                        placeholder = "যেমন: 1234",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (appLockEnabled) {
                            Button(
                                onClick = {
                                    settingsPrefs.setAppLock(false, "")
                                    showPinDialog = false
                                    Toast.makeText(context, "অ্যাপ লক বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30).copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("লক বন্ধ করুন", color = Color(0xFFFF3B30), fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                if (pinInput.length == 4) {
                                    settingsPrefs.setAppLock(true, pinInput)
                                    showPinDialog = false
                                    Toast.makeText(context, "PIN লক সফলভাবে সক্রিয় করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "অনুগ্রহ করে ৪ সংখ্যার PIN লিখুন", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).height(44.dp)
                        ) {
                            Text("পিন সেট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ৬. ডেটা রিসেট কনফার্মেশন ডায়ালগ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isWipingData) {
                    showResetDialog = false
                    wipeCloudDataAlso = false
                }
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সমস্ত ডেটা মুছে ফেলবেন?", fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                }
            },
            text = { 
                Column {
                    Text(
                        "সতর্কতা: এটি আপনার ডিভাইসের সমস্ত ট্রানজ্যাকশন, বাজার ফর্দ, রিমাইন্ডার ও নোটস মুছে ফেলবে।",
                        color = textColor,
                        fontSize = 13.5.sp
                    )
                    
                    if (currentUser != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (wipeCloudDataAlso) Color(0xFFFF3B30).copy(alpha = 0.12f) else subtextColor.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isWipingData) { wipeCloudDataAlso = !wipeCloudDataAlso }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "ক্লাউড ব্যাকআপও চিরতরে মুছুন",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (wipeCloudDataAlso) Color(0xFFFF3B30) else textColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (wipeCloudDataAlso) 
                                            "⚠️ ক্লাউড সার্ভারের সমস্ত হিসাব চিরতরে মুছে যাবে। পরবর্তীতে লগইন করলেও আর ফেরত পাওয়া যাবে না।" 
                                            else "বন্ধ রাখলে ক্লাউডে আপনার ব্যাকআপ সুরক্ষিত থাকবে (পুনরায় লগইন করে ফেরত পাবেন)।",
                                        fontSize = 11.5.sp,
                                        color = if (wipeCloudDataAlso) Color(0xFFFF3B30) else subtextColor
                                    )
                                }
                                Switch(
                                    checked = wipeCloudDataAlso,
                                    onCheckedChange = { if (!isWipingData) wipeCloudDataAlso = it },
                                    enabled = !isWipingData,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF3B30),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = subtextColor.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isWipingData) return@Button
                        isWipingData = true
                        coroutineScope.launch {
                            try {
                                val shouldWipeCloud = wipeCloudDataAlso && (currentUser != null)
                                if (shouldWipeCloud) {
                                    val cloudRes = syncManager.deleteUserCloudData(context)
                                    BackupHelper.resetAllDatabaseData(context)
                                    if (cloudRes.isSuccess) {
                                        Toast.makeText(context, "ডিভাইস ও ক্লাউড ব্যাকআপ চিরতরে মুছে ফেলা হয়েছে ✓", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "লোকাল ডেটা মুছেছে, কিন্তু ক্লাউড মুছতে কিছু সমস্যা হয়েছে", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    BackupHelper.resetAllDatabaseData(context)
                                    Toast.makeText(context, "ডিভাইসের লোকাল ডেটা রিসেট করা হয়েছে (ক্লাউড ব্যাকআপ অক্ষত)", Toast.LENGTH_SHORT).show()
                                }
                                storageInfo = BackupHelper.getStorageInfo(context)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isWipingData = false
                                showResetDialog = false
                                wipeCloudDataAlso = false
                            }
                        }
                    },
                    enabled = !isWipingData,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    if (isWipingData) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("মুছে ফেলা হচ্ছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text(if (wipeCloudDataAlso) "হ্যাঁ, ক্লাউড সহ মুছুন" else "হ্যাঁ, লোকাল মুছুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (!isWipingData) {
                            showResetDialog = false
                            wipeCloudDataAlso = false
                        }
                    },
                    enabled = !isWipingData
                ) { Text("বাতিল", color = textColor) }
            }
        )
    }

    // ৭. অ্যাবাউট ডায়ালগ
    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("হিসাব নিকাশ (Life Manager Pro)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("ভার্সন v1.0.0 Pro Edition", fontSize = 12.sp, color = subtextColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "হিসাব নিকাশ একটি প্রিমিয়াম পার্সোনাল ফাইন্যান্স ও লাইফ ম্যানেজার অ্যাপ্লিকেশন। আপনার সমস্ত ব্যক্তিগত হিসাব, ধার-দেনা, বাজার ফর্দ ও রিমাইন্ডার সম্পূর্ণ অফলাইনে ডিভাইসের মেমরিতে শতভাগ গোপনীয়তা ও নিরাপত্তার সাথে সংরক্ষিত থাকে।",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.85f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ডেভেলপার: TechHat Solutions", fontSize = 12.sp, color = Color(0xFF0A84FF), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("বন্ধ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ৮. ক্লাউড অথেন্টিকেশন ডায়ালগ (Firebase Google / Email Login)
    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = {
                showAuthDialog = false
                coroutineScope.launch {
                    val res = syncManager.sync(context)
                    if (res.isSuccess) {
                        Toast.makeText(context, "লগইন সফল! ক্লাউড ডেটা সিঙ্ক সম্পন্ন হয়েছে ✓", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // ৯. লগআউট কনফার্মেশন ডায়ালগ
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("লগআউট করতে চান?", fontWeight = FontWeight.Bold, color = textColor) },
            text = { Text("লগআউট করলে আপনার ডিভাইসের লোকাল ডেটা অক্ষুণ্ণ থাকবে। আপনি পরে পুনরায় লগইন করে সিঙ্ক করতে পারবেন।", color = textColor) },
            confirmButton = {
                Button(
                    onClick = {
                        authManager.signOut()
                        showLogoutConfirmDialog = false
                        Toast.makeText(context, "লগআউট সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
                ) {
                    Text("লগআউট", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("বাতিল", color = textColor)
                }
            }
        )
    }

    // ১০. ক্লাউড রিস্টোর ডায়ালগ
    if (showCloudRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showCloudRestoreDialog = false },
            title = { Text("ক্লাউড থেকে রিস্টোর করবেন?", fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF)) },
            text = { Text("ক্লাউড ফায়ারবেজ থেকে আপনার সমস্ত হিসাব, ওয়ালেট ও লেনদেন এই ডিভাইসে রিস্টোর ও মার্জ করা হবে। এটি চালাতে চান?", color = textColor) },
            confirmButton = {
                Button(
                    onClick = {
                        showCloudRestoreDialog = false
                        coroutineScope.launch {
                            val res = syncManager.downloadFromCloud(context)
                            if (res.isSuccess) {
                                val count = res.getOrDefault(0)
                                Toast.makeText(context, "ক্লাউড থেকে $count টি রেকর্ড সফলভাবে রিস্টোর হয়েছে ✓", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "ক্লাউড রিস্টোর ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreDialog = false }) {
                    Text("বাতিল", color = textColor)
                }
            }
        )
    }
}

/**
 * লাস্ট সিঙ্ক টাইম ফরম্যাটার (বাংলায়)
 */
fun formatSyncTime(timestamp: Long): String {
    if (timestamp <= 0L) return "এখনো কোনো সিঙ্ক হয়নি"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "এইমাত্র সিঙ্ক হয়েছে"
        diff < 3600_000L -> "${diff / 60_000L} মিনিট আগে সিঙ্ক হয়েছে"
        diff < 86400_000L -> "${diff / 3600_000L} ঘণ্টা আগে সিঙ্ক হয়েছে"
        else -> {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn", "BD"))
            "সর্বশেষ সিঙ্ক: ${sdf.format(Date(timestamp))}"
        }
    }
}

/**
 * ============================================================================
 * সেটিংসের ছোট ছোট পুনঃব্যবহারযোগ্য কম্পোনেন্টসমূহ (iOS Inset Grouped Items)
 * ============================================================================
 */
@Composable
fun IOSSettingsGroup(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(borderColor, borderColor)),
            width = 0.6.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
    )
}

@Composable
fun SettingsRowItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBg: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingText: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(subtitle, fontSize = 11.sp, color = subtextColor, maxLines = 1)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingContent != null) {
                trailingContent()
            } else {
                if (!trailingText.isNullOrBlank()) {
                    Text(trailingText, fontSize = 13.sp, color = subtextColor, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subtextColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBg: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(subtitle, fontSize = 11.sp, color = subtextColor, maxLines = 1)
                }
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

/**
 * প্রোফাইল এডিট ডায়ালগ
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhone: String,
    currentCurrency: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phoneOrEmail: String, currency: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phoneOrEmail by remember { mutableStateOf(currentPhone) }
    var currency by remember { mutableStateOf(currentCurrency) }

    val textColor = MaterialTheme.colorScheme.onSurface
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = cardBg
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("প্রোফাইল সম্পাদনা", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))

                IOSInputField(
                    title = "আপনার নাম",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "যেমন: Asad"
                )

                Spacer(modifier = Modifier.height(12.dp))

                IOSInputField(
                    title = "মোবাইল নম্বর / ইমেইল (ঐচ্ছিক)",
                    value = phoneOrEmail,
                    onValueChange = { phoneOrEmail = it },
                    placeholder = "যেমন: 01700-000000"
                )

                Spacer(modifier = Modifier.height(12.dp))

                IOSInputField(
                    title = "ডিফল্ট মুদ্রা",
                    value = currency,
                    onValueChange = { currency = it },
                    placeholder = "৳ BDT"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("বাতিল", color = textColor)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, phoneOrEmail, currency.ifBlank { "৳ BDT" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("সংরক্ষণ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

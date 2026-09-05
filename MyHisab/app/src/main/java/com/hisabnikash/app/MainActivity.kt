package com.hisabnikash.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hisabnikash.app.ui.dashboard.DashboardScreen
import com.hisabnikash.app.ui.security.AppLockScreen
import com.hisabnikash.app.ui.theme.AppTheme
import com.hisabnikash.app.ui.theme.ThemePreferences
import com.hisabnikash.app.utils.SettingsPreferences
import com.hisabnikash.app.worker.RecurringTransactionWorker
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {

    private lateinit var settingsPreferences: SettingsPreferences
    private val isUnlockedState = MutableStateFlow(false)
    private var backgroundTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        settingsPreferences = SettingsPreferences(applicationContext)
        isUnlockedState.value = !settingsPreferences.appLockEnabled.value

        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "recurring_transactions",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val dailyNotifRequest = PeriodicWorkRequestBuilder<com.hisabnikash.app.worker.DailyNotificationWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyNotifRequest
        )

        com.hisabnikash.app.utils.NotificationHelper.createNotificationChannels(applicationContext)

        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState()
            val appLockEnabled by settingsPreferences.appLockEnabled.collectAsState()
            val isUnlocked by isUnlockedState.collectAsState()

            LaunchedEffect(appLockEnabled) {
                if (!appLockEnabled) {
                    isUnlockedState.value = true
                }
            }

            AppTheme(darkTheme = isDarkTheme) {
                if (appLockEnabled && !isUnlocked) {
                    AppLockScreen(
                        onUnlockSuccess = { isUnlockedState.value = true }
                    )
                } else {
                    DashboardScreen(themePreferences = themePreferences)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onRestart() {
        super.onRestart()
        // ১৫ সেকেন্ডের বেশি ব্যাকগ্রাউন্ডে থাকলে এবং অ্যাপ লক অন থাকলে রি-লক করা
        if (settingsPreferences.appLockEnabled.value && System.currentTimeMillis() - backgroundTimestamp > 15_000L) {
            isUnlockedState.value = false
        }
    }
}


package com.hisabnikash.app.utils

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * অ্যাপের সকল সাধারণ সেটিংস ও ব্যবহারকারী প্রোফাইল ডাটা হ্যান্ডলার
 */
class SettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // প্রোফাইল তথ্য
    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "Asad") ?: "Asad")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPhoneOrEmail = MutableStateFlow(prefs.getString(KEY_USER_PHONE_EMAIL, "") ?: "")
    val userPhoneOrEmail: StateFlow<String> = _userPhoneOrEmail.asStateFlow()

    private val _userCurrency = MutableStateFlow(prefs.getString(KEY_USER_CURRENCY, "৳ BDT") ?: "৳ BDT")
    val userCurrency: StateFlow<String> = _userCurrency.asStateFlow()

    private val _userPhotoUri = MutableStateFlow(prefs.getString(KEY_USER_PHOTO_URI, null))
    val userPhotoUri: StateFlow<String?> = _userPhotoUri.asStateFlow()

    // হিসাব সেটিংস
    private val _defaultPaymentMethod = MutableStateFlow(prefs.getString(KEY_DEFAULT_PAYMENT_METHOD, "নগদ ক্যাশ") ?: "নগদ ক্যাশ")
    val defaultPaymentMethod: StateFlow<String> = _defaultPaymentMethod.asStateFlow()

    // নিরাপত্তা ও গোপনীয়তা
    private val _hideBalance = MutableStateFlow(prefs.getBoolean(KEY_HIDE_BALANCE, false))
    val hideBalance: StateFlow<Boolean> = _hideBalance.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _appLockPin = MutableStateFlow(prefs.getString(KEY_APP_LOCK_PIN, "") ?: "")
    val appLockPin: StateFlow<String> = _appLockPin.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    // অ্যাপিয়ারেন্স ও গ্লাস ইফেক্ট
    private val _glassIntensity = MutableStateFlow(prefs.getString(KEY_GLASS_INTENSITY, "Medium") ?: "Medium") // Off, Low, Medium, High
    val glassIntensity: StateFlow<String> = _glassIntensity.asStateFlow()

    // ভাষা ও ডিজিট
    private val _useBanglaDigits = MutableStateFlow(prefs.getBoolean(KEY_USE_BANGLA_DIGITS, true))
    val useBanglaDigits: StateFlow<Boolean> = _useBanglaDigits.asStateFlow()

    // নোটিফিকেশন
    private val _budgetWarningEnabled = MutableStateFlow(prefs.getBoolean(KEY_BUDGET_WARNING, true))
    val budgetWarningEnabled: StateFlow<Boolean> = _budgetWarningEnabled.asStateFlow()

    private val _dailyReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_DAILY_REMINDER, true))
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled.asStateFlow()

    // লাস্ট ব্যাকআপ টাইম
    private val _lastBackupTime = MutableStateFlow(prefs.getString(KEY_LAST_BACKUP_TIME, "কখনো নেওয়া হয়নি") ?: "কখনো নেওয়া হয়নি")
    val lastBackupTime: StateFlow<String> = _lastBackupTime.asStateFlow()

    // প্রথমবার অ্যাপ খোলার পর লগইন নোটিশ
    private val _hasShownLoginPrompt = MutableStateFlow(prefs.getBoolean(KEY_HAS_SHOWN_LOGIN_PROMPT, false))
    val hasShownLoginPrompt: StateFlow<Boolean> = _hasShownLoginPrompt.asStateFlow()

    fun setHasShownLoginPrompt(shown: Boolean) {
        _hasShownLoginPrompt.value = shown
        prefs.edit { putBoolean(KEY_HAS_SHOWN_LOGIN_PROMPT, shown) }
    }

    fun updateProfile(name: String, phoneOrEmail: String, currency: String, photoUri: String? = null) {
        _userName.value = name
        _userPhoneOrEmail.value = phoneOrEmail
        _userCurrency.value = currency
        if (photoUri != null) _userPhotoUri.value = photoUri

        prefs.edit {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PHONE_EMAIL, phoneOrEmail)
            putString(KEY_USER_CURRENCY, currency)
            if (photoUri != null) putString(KEY_USER_PHOTO_URI, photoUri)
        }
    }

    fun setHideBalance(hide: Boolean) {
        _hideBalance.value = hide
        prefs.edit { putBoolean(KEY_HIDE_BALANCE, hide) }
    }

    fun setAppLock(enabled: Boolean, pin: String = "") {
        _appLockEnabled.value = enabled
        if (pin.isNotBlank()) _appLockPin.value = pin
        prefs.edit {
            putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            if (pin.isNotBlank()) putString(KEY_APP_LOCK_PIN, pin)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun verifyPin(inputPin: String): Boolean {
        return _appLockPin.value.isNotBlank() && inputPin == _appLockPin.value
    }

    fun setGlassIntensity(intensity: String) {
        _glassIntensity.value = intensity
        prefs.edit { putString(KEY_GLASS_INTENSITY, intensity) }
    }

    fun setUseBanglaDigits(use: Boolean) {
        _useBanglaDigits.value = use
        prefs.edit { putBoolean(KEY_USE_BANGLA_DIGITS, use) }
    }

    fun setBudgetWarning(enabled: Boolean) {
        _budgetWarningEnabled.value = enabled
        prefs.edit { putBoolean(KEY_BUDGET_WARNING, enabled) }
    }

    fun setDailyReminder(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
        prefs.edit { putBoolean(KEY_DAILY_REMINDER, enabled) }
    }

    fun setLastBackupTime(timeStr: String) {
        _lastBackupTime.value = timeStr
        prefs.edit { putString(KEY_LAST_BACKUP_TIME, timeStr) }
    }

    companion object {
        private const val PREFS_NAME = "app_settings_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE_EMAIL = "user_phone_email"
        private const val KEY_USER_CURRENCY = "user_currency"
        private const val KEY_USER_PHOTO_URI = "user_photo_uri"
        private const val KEY_DEFAULT_PAYMENT_METHOD = "default_payment_method"
        private const val KEY_HIDE_BALANCE = "hide_balance"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_APP_LOCK_PIN = "app_lock_pin"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_GLASS_INTENSITY = "glass_intensity"
        private const val KEY_USE_BANGLA_DIGITS = "use_bangla_digits"
        private const val KEY_BUDGET_WARNING = "budget_warning"
        private const val KEY_DAILY_REMINDER = "daily_reminder"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_HAS_SHOWN_LOGIN_PROMPT = "has_shown_login_prompt"
    }
}

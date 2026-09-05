package com.hisabnikash.app.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    /**
     * চেক করে ডিভাইসে ফিঙ্গারপ্রিন্ট বা বায়োমেট্রিক হার্ডওয়্যার সক্রিয় ও এনরোল করা আছে কি না
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * চেক করে ডিভাইসে বায়োমেট্রিক হার্ডওয়্যার উপস্থিত আছে কি না (এনরোল করা থাকুক বা না থাকুক)
     */
    fun isHardwareAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val res = biometricManager.canAuthenticate(AUTHENTICATORS)
        return res != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                res != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    /**
     * ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক অথেন্টিকেশন ডায়ালগ প্রদর্শন করে
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "ফিঙ্গারপ্রিন্ট যাচাই",
        subtitle: String = "অ্যাপটি আনলক করতে ফিঙ্গারপ্রিন্ট স্পর্শ করুন",
        negativeButtonText: String = "PIN ব্যবহার করুন",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

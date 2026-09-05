package com.hisabnikash.app.ui.security

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.hisabnikash.app.ui.dashboard.LiquidBackgroundGlow
import com.hisabnikash.app.utils.BiometricHelper
import com.hisabnikash.app.utils.SettingsPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? FragmentActivity
    val settingsPrefs = remember { SettingsPreferences(context) }
    val biometricEnabled by settingsPrefs.biometricEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    val isBiometricReady = remember {
        BiometricHelper.isBiometricAvailable(context) && biometricEnabled
    }

    // ফাংশন: ফিঙ্গারপ্রিন্ট প্রম্পট চালু করা
    fun triggerBiometric() {
        if (activity != null && isBiometricReady) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "ফিঙ্গারপ্রিন্ট দিয়ে আনলক",
                subtitle = "হিসাব নিকাশ সুরক্ষিত দেখতে স্পর্শ করুন",
                negativeButtonText = "PIN ব্যবহার করুন",
                onSuccess = {
                    isSuccess = true
                    scope.launch {
                        delay(100)
                        onUnlockSuccess()
                    }
                },
                onError = { _, _ -> },
                onFailed = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            )
        }
    }

    // প্রথমবার স্ক্রিন লোড হলে স্বয়ংক্রিয়ভাবে ফিঙ্গারপ্রিন্ট প্রম্পট প্রদর্শন
    LaunchedEffect(Unit) {
        if (isBiometricReady) {
            delay(250)
            triggerBiometric()
        }
    }

    // পিন যাচাই লজিক
    fun checkPin(pin: String) {
        if (settingsPrefs.verifyPin(pin)) {
            isSuccess = true
            isError = false
            errorMessage = null
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            scope.launch {
                delay(120)
                onUnlockSuccess()
            }
        } else {
            isError = true
            errorMessage = "ভুল পিন! পুনরায় চেষ্টা করুন।"
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            scope.launch {
                // শেক অ্যানিমেশন
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) {
                    // Quick multi-step shake
                }
                // manual shake
                for (i in 0..2) {
                    shakeOffset.animateTo(-24f, spring(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(24f, spring(stiffness = Spring.StiffnessHigh))
                }
                shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                delay(400)
                enteredPin = ""
                isError = false
            }
        }
    }

    fun onNumberClick(digit: String) {
        if (enteredPin.length < 4 && !isSuccess && !isError) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                checkPin(newPin)
            }
        }
    }

    fun onBackspaceClick() {
        if (enteredPin.isNotEmpty() && !isSuccess && !isError) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D10))
    ) {
        LiquidBackgroundGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // শীর্ষ ব্র্যান্ডিং ও টাইটেল
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // লক আইকন
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0A84FF).copy(alpha = 0.25f),
                                    Color(0xFF5E5CE6).copy(alpha = 0.25f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Locked",
                        tint = if (isError) Color(0xFFFF3B30) else if (isSuccess) Color(0xFF34C759) else Color(0xFF0A84FF),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "হিসাব নিকাশ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (errorMessage != null) errorMessage!! else "৪-সংখ্যার গোপনীয় PIN লিখুন",
                    fontSize = 14.sp,
                    color = if (isError) Color(0xFFFF453A) else Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ৪টি PIN ডট ইন্ডিকেটর (শেক অ্যানিমেশন সহ)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        val dotColor = when {
                            isError -> Color(0xFFFF3B30)
                            isSuccess -> Color(0xFF34C759)
                            isFilled -> Color(0xFF0A84FF)
                            else -> Color.Transparent
                        }
                        val borderColor = when {
                            isError -> Color(0xFFFF3B30)
                            isSuccess -> Color(0xFF34C759)
                            isFilled -> Color(0xFF0A84FF)
                            else -> Color.White.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(1.5.dp, borderColor, CircleShape)
                        )
                    }
                }
            }

            // নিউমেরিক কিপ্যাড (Apple iOS স্টাইল)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                val digits = listOf(
                    listOf("1" to "১", "2" to "২", "3" to "৩"),
                    listOf("4" to "৪", "5" to "৫", "6" to "৬"),
                    listOf("7" to "৭", "8" to "৮", "9" to "৯")
                )

                digits.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { (en, bn) ->
                            KeypadButton(
                                primaryText = bn,
                                secondaryText = en,
                                onClick = { onNumberClick(en) }
                            )
                        }
                    }
                }

                // শেষ সারি: [ফিঙ্গারপ্রিন্ট] [0] [ব্যাকস্পেস]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ফিঙ্গারপ্রিন্ট বাটন
                    if (isBiometricReady) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF0A84FF).copy(alpha = 0.35f), CircleShape)
                                .clickable { triggerBiometric() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "ফিঙ্গারপ্রিন্ট স্ক্যান",
                                tint = Color(0xFF0A84FF),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    // 0 ডিজিট
                    KeypadButton(
                        primaryText = "০",
                        secondaryText = "0",
                        onClick = { onNumberClick("0") }
                    )

                    // ব্যাকস্পেস বাটন
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onBackspaceClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "ব্যাকস্পেস",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // পিন ভুলে গেছেন? বাটন
                TextButton(
                    onClick = { showForgotPinDialog = true }
                ) {
                    Text(
                        text = "পিন ভুলে গেছেন?",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // পিন ভুলে যাওয়ার ডায়ালগ
    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = { Text("পিন ভুলে গেছেন?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "১. আপনার ডিভাইসে ফিঙ্গারপ্রিন্ট সক্রিয় থাকলে ফিঙ্গারপ্রিন্ট আইকনে ট্যাপ করে সরাসরি আনলক করতে পারবেন।\n\n" +
                    "২. অন্যথায় অ্যাপটি আনইনস্টল করে পুনরায় ইনস্টল করলে ক্লাউড ব্যাকআপ থেকে ডেটা রিস্টোর করতে পারবেন।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForgotPinDialog = false
                        if (isBiometricReady) triggerBiometric()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
                ) {
                    Text(if (isBiometricReady) "ফিঙ্গারপ্রিন্ট ব্যবহার করুন" else "ঠিক আছে")
                }
            },
            dismissButton = {
                if (isBiometricReady) {
                    TextButton(onClick = { showForgotPinDialog = false }) {
                        Text("বন্ধ করুন")
                    }
                }
            }
        )
    }
}

@Composable
private fun KeypadButton(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = primaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = secondaryText,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

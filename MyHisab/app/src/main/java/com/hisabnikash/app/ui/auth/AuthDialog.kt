package com.hisabnikash.app.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.hisabnikash.app.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.hisabnikash.app.auth.AuthManager
import com.hisabnikash.app.sync.CloudSyncManager
import com.hisabnikash.app.ui.components.GlassCard
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val authManager = remember { AuthManager.getInstance() }
    val syncManager = remember { CloudSyncManager.getInstance() }

    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val inputBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F7)
    val titleColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val textColor = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.7f) else Color(0xFF3C3C43).copy(alpha = 0.7f)

    var isSignUpMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // গুগল সাইন-ইন কনফিগারেশন (OAuth Web Client ID)
    val webClientId = remember {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "304667443588-tnj9etpugtopm8hmvieloej718qs39v5.apps.googleusercontent.com"
        } catch (e: Exception) {
            "304667443588-tnj9etpugtopm8hmvieloej718qs39v5.apps.googleusercontent.com"
        }
    }
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    val authResult = authManager.signInWithGoogleIdToken(idToken)
                    if (authResult.isSuccess) {
                        Toast.makeText(context, "গুগল দিয়ে লগইন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                        val restoreRes = syncManager.smartSyncOnLogin(context)
                        val count = restoreRes.getOrDefault(0)
                        if (count > 0) {
                            Toast.makeText(context, "ক্লাউড থেকে $count টি হিসাব সফলভাবে ফিরিয়ে আনা হয়েছে ✓", Toast.LENGTH_LONG).show()
                        }
                        onAuthSuccess()
                        onDismiss()
                    } else {
                        errorMessage = authResult.exceptionOrNull()?.message ?: "গুগল লগইন ব্যর্থ হয়েছে"
                    }
                    isLoading = false
                }
            } else {
                isLoading = false
                errorMessage = "গুগল অ্যাকাউন্ট থেকে আইডি টোকেন পাওয়া যায়নি"
            }
        } catch (e: ApiException) {
            isLoading = false
            val errorMsg = when (e.statusCode) {
                10 -> "কনফিগারেশন ত্রুটি (Developer Error 10): গুগল প্লে সার্ভিসেস বা SHA-1 যাচাই করুন"
                12500 -> "গুগল সাইন-ইন সম্পন্ন হয়নি (Error 12500): অনুগ্রহ করে পুনরায় চেষ্টা করুন"
                7 -> "ইন্টারনেট সংযোগ বিচ্ছিন্ন (Network Error)"
                12501 -> null // ব্যবহারকারী বাতিল করেছেন
                else -> "গুগল সাইন-ইন ত্রুটি (${e.statusCode}): ${e.localizedMessage ?: "সমস্যা হয়েছে"}"
            }
            if (errorMsg != null) {
                errorMessage = errorMsg
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
            android.util.Log.e("AuthDialog", "Google sign-in ApiException code: ${e.statusCode}", e)
        } catch (e: Exception) {
            isLoading = false
            val errorMsg = "গুগল সাইন-ইন ব্যর্থ: ${e.localizedMessage ?: "সমস্যা হয়েছে"}"
            errorMessage = errorMsg
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            android.util.Log.e("AuthDialog", "Google sign-in Exception", e)
        }
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { if (!isLoading) onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // লোগো ও আইকন
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isSignUpMode) "নতুন অ্যাকাউন্ট তৈরি করুন" else "হিসাব নিকাশে স্বাগতম",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "আপনার হিসাবের খাতা অনলাইনে সুরক্ষিত রাখুন ও যেকোনো ডিভাইস থেকে অ্যাক্সেস করুন।",
                        fontSize = 12.5.sp,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // ট্যাব সুইচ (লগইন / রেজিস্ট্রেশন)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpMode) Color(0xFF0A84FF) else Color.Transparent)
                                .clickable {
                                    isSignUpMode = false
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "লগইন",
                                fontSize = 14.sp,
                                fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isSignUpMode) Color.White else textColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpMode) Color(0xFF0A84FF) else Color.Transparent)
                                .clickable {
                                    isSignUpMode = true
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "নতুন অ্যাকাউন্ট",
                                fontSize = 14.sp,
                                fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSignUpMode) Color.White else textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // গুগল সাইন-ইন বাটন
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            googleSignInClient.signOut().addOnCompleteListener {
                                val signInIntent = googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                            contentColor = titleColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "গুগল দিয়ে প্রবেশ করুন",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = textColor.copy(alpha = 0.2f))
                        Text(
                            text = " অথবা ইমেইল দিয়ে ",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = textColor.copy(alpha = 0.2f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // এরর মেসেজ ব্যানার
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
                                .border(0.6.dp, Color(0xFFFF3B30).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.5.sp,
                                color = Color(0xFFFF453A),
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // সাইন আপে নাম ফিল্ড
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("আপনার নাম") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0A84FF)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0A84FF),
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedTextColor = titleColor,
                                unfocusedTextColor = titleColor,
                                focusedLabelColor = Color(0xFF0A84FF),
                                unfocusedLabelColor = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // ইমেইল ফিল্ড
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("ইমেইল ঠিকানা") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF0A84FF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            focusedLabelColor = Color(0xFF0A84FF),
                            unfocusedLabelColor = textColor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // পাসওয়ার্ড ফিল্ড
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0A84FF)) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = textColor
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                            focusedLabelColor = Color(0xFF0A84FF),
                            unfocusedLabelColor = textColor
                        )
                    )

                    if (!isSignUpMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(onClick = { showForgotPasswordDialog = true }) {
                                Text("পাসওয়ার্ড মনে নেই?", fontSize = 12.sp, color = Color(0xFF0A84FF))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // সাবমিট বাটন
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "অনুগ্রহ করে ইমেইল ও পাসওয়ার্ড প্রদান করুন"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে"
                                return@Button
                            }

                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                val result = if (isSignUpMode) {
                                    authManager.signUpWithEmail(name, email, password)
                                } else {
                                    authManager.signInWithEmail(email, password)
                                }

                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        if (isSignUpMode) "অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!" else "লগইন সফল হয়েছে!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // ক্লাউড সিঙ্ক
                                    val restoreRes = syncManager.smartSyncOnLogin(context)
                                    val count = restoreRes.getOrDefault(0)
                                    if (count > 0) {
                                        Toast.makeText(context, "ক্লাউড থেকে $count টি হিসাব সফলভাবে ফিরিয়ে আনা হয়েছে ✓", Toast.LENGTH_LONG).show()
                                    }
                                    onAuthSuccess()
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "একটি সমস্যা হয়েছে"
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("প্রক্রিয়াধীন...", fontSize = 14.sp, color = Color.White)
                        } else {
                            Text(
                                text = if (isSignUpMode) "অ্যাকাউন্ট তৈরি করুন" else "লগইন করুন",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // স্কিপ বাটন
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "এখনই নয়, পরে করব",
                            fontSize = 13.sp,
                            color = textColor
                        )
                    }
                }
            }
        }
    }

    // পাসওয়ার্ড রিসেট ডায়ালগ
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var isSendingReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("পাসওয়ার্ড রিসেট করুন") },
            text = {
                Column {
                    Text("আপনার অ্যাকাউন্টের ইমেইল ঠিকানা দিলে পাসওয়ার্ড রিসেট করার লিঙ্ক পাঠিয়ে দেওয়া হবে।", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("ইমেইল") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isBlank()) return@Button
                        coroutineScope.launch {
                            isSendingReset = true
                            val res = authManager.sendPasswordReset(resetEmail)
                            if (res.isSuccess) {
                                Toast.makeText(context, "রিসেট লিংক পাঠানো হয়েছে! ইনবক্স চেক করুন।", Toast.LENGTH_LONG).show()
                                showForgotPasswordDialog = false
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "ত্রুটি হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                            isSendingReset = false
                        }
                    },
                    enabled = !isSendingReset
                ) {
                    Text("লিঙ্ক পাঠান")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

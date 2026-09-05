package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import android.net.Uri
import com.hisabnikash.app.data.local.PersonEntity
import com.hisabnikash.app.ui.components.GlassCard
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddPersonScreen(
    initialPerson: PersonEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, date: String, photoUri: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialPerson?.name ?: "") }
    var phone by remember { mutableStateOf(initialPerson?.phone ?: "") }
    var address by remember { mutableStateOf(initialPerson?.address ?: "") }
    var photoUri by remember { mutableStateOf<Uri?>(initialPerson?.photoUri?.let { Uri.parse(it) }) }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    var dateText by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    val isFormValid = name.isNotBlank()
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LiquidBackgroundGlow() // Using the same background glow

            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন ব্যক্তি", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val savedPhotoUri = photoUri?.let { uri ->
                                    if (uri.toString().startsWith("content://")) {
                                        ImageUtil.copyImageToInternalStorage(context, uri)?.toString()
                                    } else {
                                        uri.toString()
                                    }
                                } ?: initialPerson?.photoUri
                                onSave(name, phone, address, dateText, savedPhotoUri)
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("সেইভ", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Profile Image Placeholder
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
                        onResult = { uri ->
                            uri?.let { photoUri = it }
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF0A84FF).copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, Color(0xFF0A84FF).copy(alpha = 0.3f), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                imagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "Add Photo",
                                tint = Color(0xFF0A84FF),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Input Fields
                    PersonInputField(
                        value = name,
                        onValueChange = { name = it },
                        hint = "নাম",
                        icon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    PersonInputField(
                        value = phone,
                        onValueChange = { phone = it },
                        hint = "ফোন নম্বর (ঐচ্ছিক)",
                        icon = Icons.Default.Phone,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        trailingIcon = {
                            IconButton(onClick = {
                                android.widget.Toast.makeText(context, "Contact import coming soon", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Contacts, contentDescription = "Import Contact", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PersonInputField(
                        value = address,
                        onValueChange = { address = it },
                        hint = "ঠিকানা (ঐচ্ছিক)",
                        icon = Icons.Default.Place
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val calendar = remember { Calendar.getInstance() }
                    
                    if (showDatePicker) {
                        IOSDatePickerDialog(
                            onDismiss = { showDatePicker = false },
                            onDateSelected = { time ->
                                if (time != null) {
                                    val format = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale("bn", "BD"))
                                    dateText = format.format(java.util.Date(time))
                                }
                                showDatePicker = false
                            }
                        )
                    }

                    PersonInputField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        hint = "তারিখ",
                        icon = Icons.Default.DateRange,
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { showDatePicker = true }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val savedPhotoUri = photoUri?.let { uri ->
                                    if (uri.toString().startsWith("content://")) {
                                        ImageUtil.copyImageToInternalStorage(context, uri)?.toString()
                                    } else {
                                        uri.toString()
                                    }
                                } ?: initialPerson?.photoUri
                                onSave(name, phone, address, dateText, savedPhotoUri)
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("সেইভ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
fun PersonInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: ImageVector,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        glassAlpha = 0.05f
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                singleLine = true,
                readOnly = readOnly,
                enabled = !readOnly,
                keyboardOptions = keyboardOptions,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

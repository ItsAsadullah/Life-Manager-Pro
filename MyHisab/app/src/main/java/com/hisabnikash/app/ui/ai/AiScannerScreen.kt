package com.hisabnikash.app.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.data.local.TransactionEntity
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.TransactionViewModel
import com.hisabnikash.app.ui.dashboard.toBanglaString
import com.hisabnikash.app.ui.dashboard.toEnglishDouble
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AiScannerScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit = {}
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var scannedTitle by remember { mutableStateOf("") }
    var scannedAmountText by remember { mutableStateOf("") }
    var scannedCategory by remember { mutableStateOf("খাবার") }
    var scanSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isScanning = true
            scope.launch {
                delay(1500) // Mock OCR scanning process
                scannedTitle = "সুপারশপ বাজার রসিদ"
                scannedAmountText = "৪৫০"
                scannedCategory = "বাজার"
                isScanning = false
                scanSuccess = true
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("AI মেমো ও রসিদ স্ক্যানার", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("রসিদের ছবি তুললে স্বয়ংক্রিয় খরচ যোগ হবে", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Upload Box
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { galleryLauncher.launch("image/*") },
                shape = RoundedCornerShape(24.dp),
                glassAlpha = 0.15f
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = Color(0xFFFF9F0A))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("রসিদ স্ক্যান করা হচ্ছে...", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else if (scanSuccess) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF30D158).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("স্ক্যান সফল হয়েছে!", fontSize = 16.sp, color = Color(0xFF30D158), fontWeight = FontWeight.Bold)
                        Text("অন্য রসিদ বেছে নিতে ট্যাপ করুন", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("মেমো বা রসিদের ছবি সিলেক্ট করুন", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("গ্যালারি থেকে ফটো নির্বাচন করতে ট্যাপ করুন", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Parsed Result Card
            if (scanSuccess) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    glassAlpha = 0.15f
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("স্ক্যানকৃত বিস্তারিত তথ্য", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = scannedTitle,
                            onValueChange = { scannedTitle = it },
                            label = { Text("খাতের নাম") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = scannedAmountText,
                            onValueChange = { scannedAmountText = it },
                            label = { Text("মোট টাকার পরিমাণ (৳)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = scannedCategory,
                            onValueChange = { scannedCategory = it },
                            label = { Text("ক্যাটাগরি") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val amount = scannedAmountText.toEnglishDouble()
                                if (amount > 0) {
                                    val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))
                                    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
                                    val now = Calendar.getInstance().time
                                    viewModel.insert(
                                        TransactionEntity(
                                            title = scannedTitle.ifBlank { "মেমো খরচ" },
                                            amount = amount,
                                            isIncome = false,
                                            time = timeFormat.format(now),
                                            date = dateFormat.format(now),
                                            category = scannedCategory.ifBlank { "অন্যান্য" }
                                        )
                                    )
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("খরচ হিসেবে যুক্ত করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

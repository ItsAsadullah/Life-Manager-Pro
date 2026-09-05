package com.hisabnikash.app.ui.dashboard


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RecurringSetupBottomSheet(
    onDismissRequest: () -> Unit,
    initialPeriod: String?,
    calendar: Calendar,
    onDateOrTimeChange: () -> Unit,
    onSave: (period: String) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(initialPeriod ?: "Monthly") }

    val dateFormatMonth = SimpleDateFormat("MMMM", Locale("bn", "BD"))
    val dateFormatDay = SimpleDateFormat("dd তারিখ", Locale("bn", "BD"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismissRequest() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(32.dp),
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    glassAlpha = 0.1f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("রিকারিং সেটআপ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            }
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).clickable { onDismissRequest() }, contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "আপনার সেট করা সময় অনুযায়ী স্বয়ংক্রিয়ভাবে যোগ হবে", 
                            fontSize = 14.sp, 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Chips row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                Pair("Daily", "দৈনিক"), 
                                Pair("Monthly", "মাসিক"), 
                                Pair("Yearly", "বাৎসরিক")
                            ).forEach { (value, label) ->
                                val isSelected = selectedPeriod == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0xFF0A84FF) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                        .clickable { selectedPeriod = value },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label, 
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Pickers based on selection
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            
                            if (selectedPeriod == "Yearly") {
                                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).clickable {
                                    showDatePicker = true
                                }, contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(dateFormatMonth.format(calendar.time), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            if (selectedPeriod == "Monthly" || selectedPeriod == "Yearly") {
                                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).clickable {
                                    showDatePicker = true
                                }, contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(dateFormatDay.format(calendar.time), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            
                            if (selectedPeriod == "Daily") {
                                Box(modifier = Modifier.weight(1f).height(48.dp)) // Spacer to keep time picker size consistent or we can make it full width
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Time picker
                        Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).clickable {
                            showTimePicker = true
                        }, contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(timeFormat.format(calendar.time), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { onSave(selectedPeriod) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("সেইভ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        
        if (showDatePicker) {
            IOSDatePickerDialog(
                onDismiss = { showDatePicker = false },
                onDateSelected = { time -> 
                    if (time != null) {
                        calendar.timeInMillis = time
                        onDateOrTimeChange()
                    }
                    showDatePicker = false
                }
            )
        }

        if (showTimePicker) {
            IOSTimePickerDialog(
                onDismiss = { showTimePicker = false },
                onTimeSelected = { h, m ->
                    calendar.set(Calendar.HOUR_OF_DAY, h)
                    calendar.set(Calendar.MINUTE, m)
                    onDateOrTimeChange()
                    showTimePicker = false
                }
            )
        }
    }
}

package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.ui.components.GlassCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import java.util.Calendar

val banglaMonths = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")

fun Int.toBanglaString(): String {
    val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return this.toString().map { if (it.isDigit()) banglaDigits[it - '0'] else it }.joinToString("")
}

// ==========================================
// 0. Premium iOS 3D Wheel Picker with HAPTIC (Theme Aware)
// ==========================================
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val itemHeight = 44.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val haptic = LocalHapticFeedback.current
    
    val selectionBgColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFF007AFF).copy(alpha = 0.10f)
    }

    val itemTextColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    // Haptic Feedback Logic
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@snapshotFlow null
            val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
            layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs(it.offset + (it.size / 2f) - viewportCenter)
            }?.index
        }
        .mapNotNull { it }
        .distinctUntilChanged()
        .collect { _ ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Auto Snapping
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && items.isNotEmpty()) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            val closest = layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs(it.offset + (it.size / 2) - viewportCenter)
            }
            closest?.let {
                if (it.index in items.indices && it.index != selectedIndex) {
                    onItemSelected(it.index)
                }
                listState.animateScrollToItem(it.index)
            }
        }
    }

    Box(modifier = modifier.height(itemHeight * 5), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(selectionBgColor)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onItemSelected(index) }
                        .graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val visibleInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (visibleInfo != null) {
                                val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                                val itemCenter = visibleInfo.offset + (visibleInfo.size / 2f)
                                val distance = kotlin.math.abs(viewportCenter - itemCenter)
                                val maxDistance = itemHeight.toPx() * 2.5f
                                val fraction = (distance / maxDistance).coerceIn(0f, 1f)
                                val scale = 1f - (fraction * 0.2f)
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - (fraction * 0.6f)
                                rotationX = fraction * 45f * if (itemCenter > viewportCenter) -1f else 1f
                            } else alpha = 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(items[index]),
                        fontSize = 18.sp,
                        color = itemTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. Month Picker (মাস নির্বাচন করুন)
// ==========================================
@Composable
fun IOSMonthPickerDialog(
    onDismiss: () -> Unit,
    onMonthSelected: (monthIndex: Int, year: Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    val years = (currentYear - 20..currentYear + 20).toList()

    val titleColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val backdropColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.45f)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.White

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.1f else 0.05f
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss) { Text("বাতিল", color = Color(0xFF0A84FF), fontSize = 16.sp) }
                            Text("নির্বাচন করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                            TextButton(onClick = { onMonthSelected(selectedMonth, selectedYear) }) { Text("ঠিক আছে", color = Color(0xFF0A84FF), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            WheelPicker(
                                items = banglaMonths,
                                selectedIndex = selectedMonth,
                                onItemSelected = { selectedMonth = it },
                                itemLabel = { it },
                                modifier = Modifier.weight(1f)
                            )
                            WheelPicker(
                                items = years,
                                selectedIndex = years.indexOf(selectedYear),
                                onItemSelected = { selectedYear = years[it] },
                                itemLabel = { it.toBanglaString() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. Year Picker (বছর নির্বাচন করুন)
// ==========================================
@Composable
fun IOSYearPickerDialog(
    onDismiss: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val years = (currentYear - 20..currentYear + 20).toList()

    val titleColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val backdropColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.45f)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.White

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.1f else 0.05f
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss) { Text("বাতিল", color = Color(0xFF0A84FF), fontSize = 16.sp) }
                            Text("বছর নির্বাচন", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                            TextButton(onClick = { onYearSelected(selectedYear) }) { Text("ঠিক আছে", color = Color(0xFF0A84FF), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        WheelPicker(
                            items = years,
                            selectedIndex = years.indexOf(selectedYear),
                            onItemSelected = { selectedYear = years[it] },
                            itemLabel = { it.toBanglaString() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Date Picker (তারিখ বেছে নিন)
// ==========================================
@Composable
fun IOSDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val calendar = Calendar.getInstance()
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    
    val years = (calendar.get(Calendar.YEAR) - 20..calendar.get(Calendar.YEAR) + 20).toList()
    
    val daysInMonth = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    if (selectedDay > daysInMonth) selectedDay = daysInMonth
    val days = (1..daysInMonth).toList()

    val titleColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val backdropColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.45f)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.White

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.1f else 0.05f
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss) { Text("বাতিল", color = Color(0xFF0A84FF), fontSize = 16.sp) }
                            Text("তারিখ নির্বাচন", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                            TextButton(onClick = { 
                                val selectedCal = Calendar.getInstance()
                                selectedCal.set(selectedYear, selectedMonth, selectedDay)
                                onDateSelected(selectedCal.timeInMillis)
                            }) { Text("ঠিক আছে", color = Color(0xFF0A84FF), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Date
                            WheelPicker(
                                items = days,
                                selectedIndex = days.indexOf(selectedDay),
                                onItemSelected = { selectedDay = days[it] },
                                itemLabel = { it.toBanglaString() },
                                modifier = Modifier.weight(0.8f)
                            )
                            // Month
                            WheelPicker(
                                items = banglaMonths,
                                selectedIndex = selectedMonth,
                                onItemSelected = { selectedMonth = it },
                                itemLabel = { it },
                                modifier = Modifier.weight(1.2f)
                            )
                            // Year
                            WheelPicker(
                                items = years,
                                selectedIndex = years.indexOf(selectedYear),
                                onItemSelected = { selectedYear = years[it] },
                                itemLabel = { it.toBanglaString() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. Date Range Picker (কাস্টম রেঞ্জ)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSDateRangePickerDialog(
    onDismiss: () -> Unit,
    onRangeSelected: (Long?, Long?) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dateRangePickerState = rememberDateRangePickerState()
    val backdropColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.50f)
    val titleColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.98f) else Color.White

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = cardBg,
                glassAlpha = if (isDark) 0.1f else 0.05f
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = titleColor)
                        }
                        Text("কাস্টম রেঞ্জ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                        TextButton(onClick = {
                            onRangeSelected(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis)
                            onDismiss()
                        }) {
                            Text("সেইভ", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f),
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF3C3C43),
                            headlineContentColor = titleColor,
                            weekdayContentColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF8E8E93),
                            subheadContentColor = titleColor,
                            yearContentColor = titleColor,
                            currentYearContentColor = Color(0xFF0A84FF),
                            selectedYearContentColor = Color.White,
                            selectedYearContainerColor = Color(0xFF0A84FF),
                            dayContentColor = titleColor,
                            selectedDayContentColor = Color.White,
                            selectedDayContainerColor = Color(0xFF0A84FF),
                            todayContentColor = Color(0xFF0A84FF),
                            todayDateBorderColor = Color(0xFF0A84FF),
                            dayInSelectionRangeContainerColor = Color(0xFF0A84FF).copy(alpha = if (isDark) 0.3f else 0.15f),
                            dayInSelectionRangeContentColor = if (isDark) Color.White else Color(0xFF0A84FF)
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. Time Picker (সময় নির্বাচন করুন)
// ==========================================
@Composable
fun IOSTimePickerDialog(
    onDismiss: () -> Unit,
    initialHour: Int? = null,
    initialMinute: Int? = null,
    onTimeSelected: (hourOfDay: Int, minute: Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val calendar = Calendar.getInstance()
    val defaultHour = initialHour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val defaultMinute = initialMinute ?: calendar.get(Calendar.MINUTE)
    
    val init12Hour = when {
        defaultHour == 0 -> 12
        defaultHour > 12 -> defaultHour - 12
        else -> defaultHour
    }
    val initAmPm = if (defaultHour < 12) "AM" else "PM"

    var selectedHour by remember { mutableIntStateOf(init12Hour) }
    var selectedMinute by remember { mutableIntStateOf(defaultMinute) }
    var selectedAmPm by remember { mutableStateOf(initAmPm) }
    
    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val amPmList = listOf("AM", "PM")

    val titleColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val backdropColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.45f)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.White

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.1f else 0.05f
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss) { Text("বাতিল", color = Color(0xFF0A84FF), fontSize = 16.sp) }
                            Text("সময় নির্বাচন করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                            TextButton(onClick = { 
                                var finalHour = selectedHour
                                if (selectedAmPm == "AM" && finalHour == 12) finalHour = 0
                                else if (selectedAmPm == "PM" && finalHour < 12) finalHour += 12
                                onTimeSelected(finalHour, selectedMinute) 
                            }) { Text("ঠিক আছে", color = Color(0xFF0A84FF), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Hour
                            WheelPicker(
                                items = hours,
                                selectedIndex = hours.indexOf(selectedHour),
                                onItemSelected = { selectedHour = hours[it] },
                                itemLabel = { it.toString().padStart(2, '0').map { c -> if (c.isDigit()) arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')[c - '0'] else c }.joinToString("") },
                                modifier = Modifier.weight(1f)
                            )
                            // Minute
                            WheelPicker(
                                items = minutes,
                                selectedIndex = minutes.indexOf(selectedMinute),
                                onItemSelected = { selectedMinute = minutes[it] },
                                itemLabel = { it.toString().padStart(2, '0').map { c -> if (c.isDigit()) arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')[c - '0'] else c }.joinToString("") },
                                modifier = Modifier.weight(1f)
                            )
                            // AM/PM
                            WheelPicker(
                                items = amPmList,
                                selectedIndex = amPmList.indexOf(selectedAmPm),
                                onItemSelected = { selectedAmPm = amPmList[it] },
                                itemLabel = { it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

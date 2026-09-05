package com.hisabnikash.app.ui.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.data.local.ReminderEntity
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.EmptyStateContent
import com.hisabnikash.app.ui.dashboard.IOSDatePickerDialog
import com.hisabnikash.app.ui.dashboard.TransactionViewModel
import com.hisabnikash.app.ui.dashboard.banglaMonths
import com.hisabnikash.app.ui.dashboard.toBanglaString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * বাংলা তারিখ ফরম্যাট হেল্পার
 */
fun formatBanglaDate(day: Int, monthIndex: Int, year: Int): String {
    val m = banglaMonths.getOrElse(monthIndex) { "সেপ্টেম্বর" }
    return "${day.toBanglaString()} $m, ${year.toBanglaString()}"
}

/**
 * বিভিন্ন ফরম্যাটে সেভ থাকা তারিখের সাথে মেলাতে সাহায্য করে
 */
fun isReminderOnDate(dateStr: String, day: Int, monthIndex: Int, year: Int): Boolean {
    val canonical = formatBanglaDate(day, monthIndex, year)
    if (dateStr == canonical) return true

    val dayStr = day.toBanglaString()
    val dayPadStr = if (day < 10) "০${day.toBanglaString()}" else dayStr
    val yearStr = year.toBanglaString()
    val monthFull = banglaMonths.getOrElse(monthIndex) { "" }
    val monthShort = when (monthIndex) {
        0 -> "জানু"
        1 -> "ফেব্রু"
        2 -> "মার্চ"
        3 -> "এপ্রিল"
        4 -> "মে"
        5 -> "জুন"
        6 -> "জুলাই"
        7 -> "আগস্ট"
        8 -> "সেপ"
        9 -> "অক্টো"
        10 -> "নভে"
        11 -> "ডিসে"
        else -> ""
    }

    val hasDay = dateStr.contains(dayStr) || dateStr.contains(dayPadStr)
    val hasYear = dateStr.contains(yearStr)
    val hasMonth = dateStr.contains(monthFull) || (monthShort.isNotBlank() && dateStr.contains(monthShort))

    return hasDay && hasYear && hasMonth
}

@Composable
fun RemindersScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit = {}
) {
    val reminders by viewModel.allReminders.collectAsState()
    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = আসন্ন, 1 = আজকের, 2 = ক্যালেন্ডার 📅, 3 = সম্পন্ন
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<ReminderEntity?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)

    // বর্তমান ক্যালেন্ডার রেফারেন্স
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayCanonicalDate = remember { formatBanglaDate(todayDay, todayMonth, todayYear) }

    // ক্যালেন্ডার ভিউ স্টেট
    var viewYear by remember { mutableIntStateOf(todayYear) }
    var viewMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    val selectedCalendarDate = remember(selectedDay, viewMonth, viewYear) {
        formatBanglaDate(selectedDay, viewMonth, viewYear)
    }

    // ফিল্টারকৃত তালিকা
    val now = System.currentTimeMillis()
    val upcomingReminders = reminders.filter { !it.isCompleted && it.timestamp >= now }
    val todayReminders = reminders.filter { !it.isCompleted && isReminderOnDate(it.eventDate, todayDay, todayMonth, todayYear) }
    val completedReminders = reminders.filter { it.isCompleted }
    val calendarSelectedReminders = reminders.filter { isReminderOnDate(it.eventDate, selectedDay, viewMonth, viewYear) }

    val displayedReminders = when (selectedSegment) {
        0 -> upcomingReminders
        1 -> todayReminders
        2 -> calendarSelectedReminders
        3 -> completedReminders
        else -> reminders
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0A84FF),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন রিমাইন্ডার", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ১. শীর্ষ নেভিগেশন বার
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("রিমাইন্ডার ও ইভেন্ট", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = textColor)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A84FF).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${reminders.size.toLong().toBanglaString()} টি মোট",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A84FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ২. iOS সেগমেন্টেড কন্ট্রোল
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .padding(3.dp)
            ) {
                val segments = listOf(
                    "আসন্ন (${upcomingReminders.size.toLong().toBanglaString()})",
                    "আজকের (${todayReminders.size.toLong().toBanglaString()})",
                    "ক্যালেন্ডার 📅",
                    "সম্পন্ন (${completedReminders.size.toLong().toBanglaString()})"
                )
                segments.forEachIndexed { index, title ->
                    val isSelected = selectedSegment == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF0A84FF) else Color.Transparent)
                            .clickable { selectedSegment = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else subtextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ৩. ক্যালেন্ডার ভিউ সেকশন (যদি ক্যালেন্ডার ট্যাব সিলেক্ট করা থাকে)
            if (selectedSegment == 2) {
                IOSMonthlyCalendarView(
                    viewYear = viewYear,
                    viewMonth = viewMonth,
                    selectedDay = selectedDay,
                    todayYear = todayYear,
                    todayMonth = todayMonth,
                    todayDay = todayDay,
                    reminders = reminders,
                    isDark = isDark,
                    onOpenDatePicker = { showDatePickerDialog = true },
                    onPrevMonth = {
                        if (viewMonth == 0) {
                            viewMonth = 11
                            viewYear -= 1
                        } else {
                            viewMonth -= 1
                        }
                    },
                    onNextMonth = {
                        if (viewMonth == 11) {
                            viewMonth = 0
                            viewYear += 1
                        } else {
                            viewMonth += 1
                        }
                    },
                    onSelectDay = { day ->
                        selectedDay = day
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // নির্বাচিত তারিখের ইভেন্ট হেডার
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 $selectedCalendarDate এর ইভেন্ট",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${calendarSelectedReminders.size.toLong().toBanglaString()} টি পাওয়া গেছে",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (calendarSelectedReminders.isNotEmpty()) Color(0xFF0A84FF) else subtextColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            } else {
                // সামারি কার্ড
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.12f else 0.05f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("আসন্ন ইভেন্ট", fontSize = 11.sp, color = subtextColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${upcomingReminders.size.toLong().toBanglaString()} টি", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("আজকের রিমাইন্ডার", fontSize = 11.sp, color = subtextColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${todayReminders.size.toLong().toBanglaString()} টি", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("সম্পন্ন হয়েছে", fontSize = 11.sp, color = subtextColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${completedReminders.size.toLong().toBanglaString()} টি", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9F0A))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ৪. রিমাইন্ডার তালিকা (Apple Reminders Inset Grouped Table)
            if (displayedReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateContent(
                        icon = Icons.Default.NotificationsActive,
                        title = when (selectedSegment) {
                            0 -> "কোনো আসন্ন রিমাইন্ডার নেই"
                            1 -> "আজকের কোনো ইভেন্ট নেই"
                            2 -> "$selectedCalendarDate এ কোনো ইভেন্ট নেই"
                            else -> "কোনো সম্পন্ন রিমাইন্ডার নেই"
                        },
                        subtitle = "নিচের + বাটনে ট্যাপ করে নতুন ইভেন্ট বা রিমাইন্ডার যোগ করুন"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = cardBg,
                            glassAlpha = if (isDark) 0.12f else 0.05f
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                displayedReminders.forEachIndexed { index, reminder ->
                                    ReminderRowItem(
                                        reminder = reminder,
                                        isDark = isDark,
                                        onToggleCompleted = { viewModel.toggleReminderCompleted(reminder) },
                                        onToggleEnabled = { viewModel.toggleReminderEnabled(reminder) },
                                        onEdit = { reminderToEdit = reminder },
                                        onDelete = { viewModel.deleteReminder(reminder) }
                                    )
                                    if (index < displayedReminders.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 52.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // নতুন রিমাইন্ডার যোগ ডায়ালগ
    if (showAddDialog) {
        AddOrEditReminderDialog(
            initialReminder = null,
            initialDate = if (selectedSegment == 2) selectedCalendarDate else null,
            isDark = isDark,
            onDismiss = { showAddDialog = false },
            onSave = { newReminder ->
                viewModel.insertReminder(newReminder)
                showAddDialog = false
            }
        )
    }

    // রিমাইন্ডার এডিট ডায়ালগ
    if (reminderToEdit != null) {
        AddOrEditReminderDialog(
            initialReminder = reminderToEdit,
            isDark = isDark,
            onDismiss = { reminderToEdit = null },
            onSave = { updated ->
                viewModel.updateReminder(updated)
                reminderToEdit = null
            }
        )
    }

    // কাস্টম iOS ডেট পিকার ডায়ালগ (মাস ও সালের উপর ক্লিকে ওপেন হবে)
    if (showDatePickerDialog) {
        IOSDatePickerDialog(
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { timeInMillis ->
                if (timeInMillis != null) {
                    val cal = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
                    viewYear = cal.get(Calendar.YEAR)
                    viewMonth = cal.get(Calendar.MONTH)
                    selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                }
                showDatePickerDialog = false
            }
        )
    }
}

/**
 * ============================================================================
 * পিওর Apple iOS মান্থলি গ্রিড ক্যালেন্ডার ভিউ (7-কলাম গ্রিড + ডেটপিকার ট্রিগার)
 * ============================================================================
 */
@Composable
fun IOSMonthlyCalendarView(
    viewYear: Int,
    viewMonth: Int,
    selectedDay: Int,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    reminders: List<ReminderEntity>,
    isDark: Boolean,
    onOpenDatePicker: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)

    val firstDayCal = remember(viewYear, viewMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1 = রবি, 2 = সোম... 7 = শনি
    val leadingEmptyDays = firstDayOfWeek - 1

    // পূর্বের মাসের দিনের সংখ্যা
    val prevMonthCal = remember(viewYear, viewMonth) {
        (firstDayCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    }
    val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val monthName = banglaMonths.getOrElse(viewMonth) { "সেপ্টেম্বর" }
    val yearStr = viewYear.toBanglaString()

    val weekdays = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = cardBg,
        glassAlpha = if (isDark) 0.12f else 0.05f
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {

            // ১. হেডার: মাস ও সাল (ক্লিক করলে আমাদের কাস্টম iOS ডেট পিকার খুলবে)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // পূর্ববর্তী মাস বাটন
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable { onPrevMonth() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 🎯 মাস ও সালের উপরে ক্লিক করলে কাস্টম iOS Date Picker ওপেন হবে
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable { onOpenDatePicker() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$monthName, $yearStr",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Open Date Picker",
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // পরবর্তী মাস বাটন
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable { onNextMonth() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ২. সপ্তাহের দিনসমূহ (রবি - শনি ৭-কলাম)
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subtextColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ৩. মাসিক ৭-কলাম গ্রিড (Apple Calendar Grid)
            val totalCells = leadingEmptyDays + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - leadingEmptyDays + 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..daysInMonth) {
                                    val isToday = (viewYear == todayYear && viewMonth == todayMonth && dayNum == todayDay)
                                    val isSelected = (dayNum == selectedDay)
                                    val hasEvents = reminders.any { isReminderOnDate(it.eventDate, dayNum, viewMonth, viewYear) }

                                    Column(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF0A84FF)
                                                    isToday -> Color(0xFFFF3B30)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onSelectDay(dayNum) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toBanglaString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected || isToday -> Color.White
                                                else -> textColor
                                            }
                                        )

                                        // ইভেন্ট উপস্থিতি ডট ব্যাজ
                                        if (hasEvents) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected || isToday) Color.White
                                                        else Color(0xFFFF9F0A)
                                                    )
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                } else if (dayNum <= 0) {
                                    // পূর্বের মাসের দিনের সংখ্যা (Muted)
                                    val prevDay = daysInPrevMonth + dayNum
                                    Text(
                                        text = prevDay.toBanglaString(),
                                        fontSize = 12.sp,
                                        color = subtextColor.copy(alpha = 0.25f),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    // পরবর্তী মাসের দিনের সংখ্যা (Muted)
                                    val nextDay = dayNum - daysInMonth
                                    Text(
                                        text = nextDay.toBanglaString(),
                                        fontSize = 12.sp,
                                        color = subtextColor.copy(alpha = 0.25f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ============================================================================
 * একক রিমাইন্ডার আইটেম রো (Apple Inset Grouped Table Row)
 * ============================================================================
 */
@Composable
fun ReminderRowItem(
    reminder: ReminderEntity,
    isDark: Boolean,
    onToggleCompleted: () -> Unit,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isAlarm = reminder.alertType == "ALARM"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // সম্পন্ন করার চেকবক্স
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggleCompleted() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF30D158),
                    uncheckedColor = subtextColor.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reminder.isCompleted) subtextColor.copy(alpha = 0.5f) else textColor,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // অ্যালার্ট মোড ব্যাজ (⏰ এ্যালার্ম বনাম 🔔 নোটিফিকেশন)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isAlarm) Color(0xFFFF9F0A).copy(alpha = 0.15f)
                                else Color(0xFF0A84FF).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAlarm) "⏰ এ্যালার্ম" else "🔔 নোটিফিকেশন",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlarm) Color(0xFFFF9F0A) else Color(0xFF0A84FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📅 ${reminder.eventDate} • ⏰ ${reminder.eventTime}",
                        fontSize = 11.sp,
                        color = subtextColor
                    )
                    if (reminder.advanceNotice != "নির্দিষ্ট সময়ে") {
                        Text(
                            text = "(${reminder.advanceNotice})",
                            fontSize = 10.sp,
                            color = Color(0xFF30D158),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (reminder.note.isNotBlank()) {
                    Text(
                        text = "📝 ${reminder.note}",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (reminder.category.isNotBlank() && reminder.category != "অন্যান্য 📦") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reminder.category,
                        fontSize = 10.sp,
                        color = subtextColor.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // সুইচ ও অ্যাকশন বাটন
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = reminder.isEnabled && !reminder.isCompleted,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = if (isAlarm) Color(0xFFFF9F0A) else Color(0xFF30D158),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                modifier = Modifier.size(36.dp).padding(end = 4.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
            }
        }
    }
}

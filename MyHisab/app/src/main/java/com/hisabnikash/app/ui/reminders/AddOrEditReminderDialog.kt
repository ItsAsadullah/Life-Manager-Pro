package com.hisabnikash.app.ui.reminders
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.hisabnikash.app.data.local.ReminderEntity
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.IOSDatePickerDialog
import com.hisabnikash.app.ui.dashboard.IOSTimePickerDialog
import com.hisabnikash.app.ui.dashboard.banglaMonths
import com.hisabnikash.app.ui.dashboard.toBanglaString
import com.hisabnikash.app.ui.market.IOSInputField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val ReminderCategories = listOf(
    "ব্যক্তিগত 👤", "বিল/পেমেন্ট 💳", "অফিস 💼", "ঔষধ 💊", "বার্ষিকী 🎂", "বাজার 🛒", "অন্যান্য 📦"
)

data class AdvanceNoticeOption(
    val title: String,
    val minutes: Long
)

val AdvanceNoticeOptions = listOf(
    AdvanceNoticeOption("নির্দিষ্ট সময়ে", 0L),
    AdvanceNoticeOption("৫ মিনিট আগে", 5L),
    AdvanceNoticeOption("১৫ মিনিট আগে", 15L),
    AdvanceNoticeOption("৩০ মিনিট আগে", 30L),
    AdvanceNoticeOption("১ ঘণ্টা আগে", 60L),
    AdvanceNoticeOption("১ দিন আগে", 1440L),
    AdvanceNoticeOption("২ দিন আগে", 2880L),
    AdvanceNoticeOption("১ সপ্তাহ আগে", 10080L)
)

val RepeatOptions = listOf(
    "একবার", "প্রতিদিন", "সাপ্তাহিক", "মাসিক", "বার্ষিক"
)

@Composable
fun AddOrEditReminderDialog(
    initialReminder: ReminderEntity? = null,
    initialDate: String? = null,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val context = LocalContext.current
    val cal = remember {
        Calendar.getInstance().apply {
            if (initialReminder != null) {
                timeInMillis = initialReminder.timestamp
            }
        }
    }

    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var note by remember { mutableStateOf(initialReminder?.note ?: "") }

    val defaultDate = initialDate ?: SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(cal.time)
    var selectedDate by remember { mutableStateOf(initialReminder?.eventDate ?: defaultDate) }
    var selectedTime by remember {
        mutableStateOf(
            initialReminder?.eventTime ?: String.format(Locale.getDefault(), "%02d:%02d %s",
                if (cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR),
                cal.get(Calendar.MINUTE),
                if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            )
        )
    }

    var selectedAdvanceNotice by remember {
        mutableStateOf(
            AdvanceNoticeOptions.find { it.minutes == (initialReminder?.advanceNoticeMinutes ?: 0L) }
                ?: AdvanceNoticeOptions.first()
        )
    }

    var alertType by remember { mutableStateOf(initialReminder?.alertType ?: "NOTIFICATION") } // "NOTIFICATION" or "ALARM"
    var selectedCategory by remember { mutableStateOf(initialReminder?.category ?: ReminderCategories.first()) }
    var selectedRepeat by remember { mutableStateOf(initialReminder?.repeatInterval ?: "একবার") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.12f else 0.04f
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // হেডার
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (alertType == "ALARM") Icons.Default.Alarm else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (alertType == "ALARM") Color(0xFFFF9F0A) else Color(0xFF0A84FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (initialReminder == null) "নতুন রিমাইন্ডার" else "রিমাইন্ডার সম্পাদনা",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ১. রিমাইন্ডার টাইটেল
                        IOSInputField(
                            title = "ইভেন্ট / রিমাইন্ডার টাইটেল",
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "যেমন: বিদ্যুৎ বিল পরিশোধ, ডাক্তারের কাছে যাওয়া",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // ২. তারিখ ও সময়
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // তারিখ
                            Column(modifier = Modifier.weight(1f)) {
                                Text("তারিখ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hintColor)
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                                        .clickable { showDatePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 11.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(selectedDate, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // সময়
                            Column(modifier = Modifier.weight(1f)) {
                                Text("সময়", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hintColor)
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                                        .clickable { showTimePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 11.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(selectedTime, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Default.AccessTime, null, tint = Color(0xFF30D158), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ৩. এ্যালার্ম বনাম নোটিফিকেশন মোড সুইচ
                        Text("অ্যালার্ট মোড:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hintColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .padding(4.dp)
                        ) {
                            val isNotif = alertType == "NOTIFICATION"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isNotif) Color(0xFF0A84FF) else Color.Transparent)
                                    .clickable { alertType = "NOTIFICATION" },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notifications, null, tint = if (isNotif) Color.White else hintColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🔔 নোটিফিকেশন", fontSize = 13.sp, fontWeight = if (isNotif) FontWeight.Bold else FontWeight.Medium, color = if (isNotif) Color.White else textColor)
                                }
                            }

                            val isAlarm = alertType == "ALARM"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAlarm) Color(0xFFFF9F0A) else Color.Transparent)
                                    .clickable { alertType = "ALARM" },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Alarm, null, tint = if (isAlarm) Color.White else hintColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⏰ ডিফল্ট এ্যালার্ম", fontSize = 13.sp, fontWeight = if (isAlarm) FontWeight.Bold else FontWeight.Medium, color = if (isAlarm) Color.White else textColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ৪. অগ্রিম নোটিশ (Advance Notice Offset)
                        Text("কখন মনে করিয়ে দেবে (অগ্রিম নোটিশ):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hintColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AdvanceNoticeOptions.forEach { opt ->
                                val isSelected = selectedAdvanceNotice.minutes == opt.minutes
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF30D158)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { selectedAdvanceNotice = opt }
                                        .padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = opt.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ৫. পুনরাবৃত্তি (Repeat)
                        Text("পুনরাবৃত্তি (Repeat):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hintColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RepeatOptions.forEach { rep ->
                                val isSelected = selectedRepeat == rep
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0A84FF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { selectedRepeat = rep }
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = rep,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ৬. ক্যাটাগরি চিপস
                        Text("ক্যাটাগরি:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hintColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReminderCategories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0A84FF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ৭. বিস্তারিত নোট
                        IOSInputField(
                            title = "বিস্তারিত নোট (ঐচ্ছিক)",
                            value = note,
                            onValueChange = { note = it },
                            placeholder = "প্রয়োজনীয় তথ্য বা বিবরণ লিখুন",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // বাটনদ্বয়
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("বাতিল", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        val eventTimestamp = cal.timeInMillis
                                        val triggerTime = eventTimestamp - (selectedAdvanceNotice.minutes * 60 * 1000)

                                        val newReminder = initialReminder?.copy(
                                            title = title,
                                            note = note,
                                            eventDate = selectedDate,
                                            eventTime = selectedTime,
                                            timestamp = eventTimestamp,
                                            triggerTimestamp = triggerTime,
                                            advanceNotice = selectedAdvanceNotice.title,
                                            advanceNoticeMinutes = selectedAdvanceNotice.minutes,
                                            alertType = alertType,
                                            category = selectedCategory,
                                            repeatInterval = selectedRepeat
                                        ) ?: ReminderEntity(
                                            title = title,
                                            note = note,
                                            eventDate = selectedDate,
                                            eventTime = selectedTime,
                                            timestamp = eventTimestamp,
                                            triggerTimestamp = triggerTime,
                                            advanceNotice = selectedAdvanceNotice.title,
                                            advanceNoticeMinutes = selectedAdvanceNotice.minutes,
                                            alertType = alertType,
                                            category = selectedCategory,
                                            repeatInterval = selectedRepeat
                                        )
                                        onSave(newReminder)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (alertType == "ALARM") Color(0xFFFF9F0A) else Color(0xFF0A84FF)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(if (initialReminder == null) "সংরক্ষণ করুন" else "আপডেট করুন", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        IOSDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                if (millis != null) {
                    val dateCal = Calendar.getInstance().apply { timeInMillis = millis }
                    cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                    selectedDate = "${dateCal.get(Calendar.DAY_OF_MONTH).toBanglaString()} ${banglaMonths[dateCal.get(Calendar.MONTH)]}, ${dateCal.get(Calendar.YEAR).toBanglaString()}"
                }
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        IOSTimePickerDialog(
            onDismiss = { showTimePicker = false },
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            onTimeSelected = { hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val h = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
                showTimePicker = false
            }
        )
    }
}

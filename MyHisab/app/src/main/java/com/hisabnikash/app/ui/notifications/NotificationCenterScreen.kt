package com.hisabnikash.app.ui.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.data.local.NotificationEntity
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.TransactionViewModel
import com.hisabnikash.app.utils.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onNavigateToReminders: () -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val notifications by viewModel.allNotifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, REMINDER, DAILY, FINANCE
    var showClearAllDialog by remember { mutableStateOf(false) }

    // ফিল্টারকৃত তালিকা
    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "REMINDER" -> notifications.filter { it.type == "REMINDER" }
            "DAILY" -> notifications.filter { it.type in listOf("DAILY_MORNING", "DAILY_AFTERNOON", "DAILY_NIGHT") }
            "FINANCE" -> notifications.filter { it.type in listOf("BUDGET_ALERT", "RECURRING_TRANSACTION") }
            else -> notifications
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // ১. টপ হেডার
            NotificationHeader(
                    isDark = isDark,
                    unreadCount = unreadCount,
                    totalCount = notifications.size,
                    onBack = onBack,
                    onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                    onClearAll = { showClearAllDialog = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ২. ফিল্টার ট্যাব চিপস
                FilterTabsRow(
                    isDark = isDark,
                    selectedFilter = selectedFilter,
                    allCount = notifications.size,
                    reminderCount = notifications.count { it.type == "REMINDER" },
                    dailyCount = notifications.count { it.type in listOf("DAILY_MORNING", "DAILY_AFTERNOON", "DAILY_NIGHT") },
                    financeCount = notifications.count { it.type in listOf("BUDGET_ALERT", "RECURRING_TRANSACTION") },
                    onFilterSelected = { selectedFilter = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ৩. নোটিফিকেশন লিস্ট অথবা খালি অবস্থা
                if (filteredList.isEmpty()) {
                    EmptyNotificationView(
                        isDark = isDark,
                        selectedFilter = selectedFilter
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        itemsIndexed(filteredList, key = { _, item -> item.id }) { index, item ->
                            NotificationCard(
                                notification = item,
                                serialNumber = toBanglaDigits(index + 1),
                                isDark = isDark,
                                onCardClick = {
                                    // কার্ডে ট্যাপ করলে শুধুমাত্র পড়া হবে, নোটিফিকেশন স্ক্রিন থেকে বের হবে না
                                    viewModel.markNotificationAsRead(item.id)
                                },
                                onActionClick = { route ->
                                    // নির্দিষ্ট অ্যাকশন বাটনে চাপলে তবেই নেভিগেট করবে
                                    viewModel.markNotificationAsRead(item.id)
                                    when (route) {
                                        "reminders" -> { onBack(); onNavigateToReminders() }
                                        "budget" -> { onBack(); onNavigateToBudget() }
                                        "dashboard" -> { onBack(); onNavigateToDashboard() }
                                    }
                                },
                                onDelete = { viewModel.deleteNotification(item.id) }
                            )
                        }
                    }
                }
            }
        }

    // ক্লিয়ার অল কনফার্মেশন ডায়ালগ
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("সব নোটিফিকেশন মুছবেন?") },
            text = { Text("আপনার সকল নোটিফিকেশন হিস্ট্রি মুছে ফেলা হবে। আপনি কি নিশ্চিত?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllNotifications()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF453A))
                ) {
                    Text("হ্যাঁ, সব মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun NotificationHeader(
    isDark: Boolean,
    unreadCount: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit
) {
    val titleColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color.LightGray else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = titleColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "নোটিফিকেশন সেন্টার",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = if (unreadCount > 0) "${toBanglaDigits(unreadCount)}টি অপঠিত নোটিফিকেশন" else "সব নোটিফিকেশন পড়া হয়েছে",
                    fontSize = 12.sp,
                    color = if (unreadCount > 0) Color(0xFF0A84FF) else subtitleColor
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (unreadCount > 0) {
                IconButton(onClick = onMarkAllAsRead) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all read",
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (totalCount > 0) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = if (isDark) Color.LightGray else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabsRow(
    isDark: Boolean,
    selectedFilter: String,
    allCount: Int,
    reminderCount: Int,
    dailyCount: Int,
    financeCount: Int,
    onFilterSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            label = "সকল (${toBanglaDigits(allCount)})",
            isSelected = selectedFilter == "ALL",
            isDark = isDark,
            onClick = { onFilterSelected("ALL") }
        )
        FilterChip(
            label = "রিমাইন্ডার ⏰ (${toBanglaDigits(reminderCount)})",
            isSelected = selectedFilter == "REMINDER",
            isDark = isDark,
            onClick = { onFilterSelected("REMINDER") }
        )
        FilterChip(
            label = "দৈনিক আপডেট ☀️ (${toBanglaDigits(dailyCount)})",
            isSelected = selectedFilter == "DAILY",
            isDark = isDark,
            onClick = { onFilterSelected("DAILY") }
        )
        FilterChip(
            label = "বাজেট ও খরচ ⚠️ (${toBanglaDigits(financeCount)})",
            isSelected = selectedFilter == "FINANCE",
            isDark = isDark,
            onClick = { onFilterSelected("FINANCE") }
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeBg = Color(0xFF0A84FF)
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val activeText = Color.White
    val inactiveText = if (isDark) Color.LightGray else Color.DarkGray

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) activeBg else inactiveBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) activeText else inactiveText
        )
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationEntity,
    serialNumber: String,
    isDark: Boolean,
    onCardClick: () -> Unit,
    onActionClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val titleColor = if (isDark) Color.White else Color.Black
    val textColor = if (isDark) Color.LightGray else Color(0xFF4A4A4A)

    val (icon, gradientColors, typeLabel) = getNotificationVisuals(notification.type)
    var isExpanded by remember(notification.id) { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isExpanded = !isExpanded
                onCardClick()
            },
        shape = RoundedCornerShape(18.dp),
        glassAlpha = if (isDark) {
            if (!notification.isRead) 0.22f else 0.12f
        } else {
            if (!notification.isRead) 0.95f else 0.85f
        },
        borderAlphaHigh = if (!notification.isRead) 0.8f else 0.4f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ক্যাটাগরি আইকন উইথ গ্রেডিয়েন্ট
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // টেক্সট কনটেন্ট
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(gradientColors.first().copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$serialNumber",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = gradientColors.first()
                            )
                        }
                        Text(
                            text = typeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = gradientColors.first()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatBengaliTime(notification.timestamp),
                            fontSize = 11.sp,
                            color = if (isDark) Color.Gray else Color.DarkGray
                        )

                        // অপঠিত ডট নির্দেশক
                        if (!notification.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = textColor,
                    lineHeight = 18.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )

                // অ্যাকশন বাটন যদি কোনো রাউট থাকে (শুধু এখানে চাপলে নেভিগেট হবে)
                if (notification.actionRoute != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(gradientColors.first().copy(alpha = 0.15f))
                            .clickable { onActionClick(notification.actionRoute) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = getActionRouteLabel(notification.actionRoute),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = gradientColors.first()
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Go",
                            tint = gradientColors.first(),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // ডিলিট বাটন
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = if (isDark) Color.Gray.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationView(
    isDark: Boolean,
    selectedFilter: String
) {
    val titleColor = if (isDark) Color.White else Color.Black
    val textColor = if (isDark) Color.LightGray else Color.Gray

    val emptyMessage = when (selectedFilter) {
        "REMINDER" -> "কোনো রিমাইন্ডার নোটিফিকেশন নেই।"
        "DAILY" -> "দৈনিক সকাল, দুপুর বা রাতের নোটিফিকেশন সময়মতো এখানে প্রদর্শিত হবে।"
        "FINANCE" -> "কোনো বাজেট সতর্কতা বা লেনদেন নোটিফিকেশন নেই।"
        else -> "আপনার সকল রিমাইন্ডার, দৈনন্দিন বাজেট আপডেট ও নোটিফিকেশন এখানে সংরক্ষিত থাকবে।"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "Empty",
                tint = Color(0xFF0A84FF),
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "কোনো নতুন নোটিফিকেশন নেই",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = emptyMessage,
            fontSize = 13.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 40.dp),
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun getNotificationVisuals(type: String): Triple<ImageVector, List<Color>, String> {
    return when (type) {
        "DAILY_MORNING" -> Triple(
            Icons.Default.WbSunny,
            listOf(Color(0xFFFF9500), Color(0xFFFFCC00)),
            "দৈনিক সকাল ☀️"
        )
        "DAILY_AFTERNOON" -> Triple(
            Icons.Default.Coffee,
            listOf(Color(0xFFFF5E3A), Color(0xFFFF9500)),
            "দৈনিক দুপুর ☕"
        )
        "DAILY_NIGHT" -> Triple(
            Icons.Default.Bedtime,
            listOf(Color(0xFF5856D6), Color(0xFFAF52DE)),
            "সারাদিনের হিসাব 🌙"
        )
        "REMINDER" -> Triple(
            Icons.Default.Alarm,
            listOf(Color(0xFF0A84FF), Color(0xFF5AC8FA)),
            "রিমাইন্ডার ⏰"
        )
        "BUDGET_ALERT" -> Triple(
            Icons.Default.Warning,
            listOf(Color(0xFFFF3B30), Color(0xFFFF453A)),
            "বাজেট সতর্কতা ⚠️"
        )
        "RECURRING_TRANSACTION" -> Triple(
            Icons.Default.SyncAlt,
            listOf(Color(0xFF30D158), Color(0xFF34C759)),
            "স্বয়ংক্রিয় লেনদেন 🔄"
        )
        else -> Triple(
            Icons.Default.Notifications,
            listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)),
            "নোটিফিকেশন 🔔"
        )
    }
}

private fun getActionRouteLabel(route: String?): String {
    return when (route) {
        "reminders" -> "রিমাইন্ডারে যান"
        "budget" -> "বাজেটে যান"
        "dashboard" -> "হিসাবে যান"
        else -> "বিস্তারিত দেখুন"
    }
}

private fun toBanglaDigits(input: Any?): String {
    if (input == null) return ""
    val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return input.toString().map { if (it in '0'..'9') banglaDigits[it - '0'] else it }.joinToString("")
}

private fun formatBengaliTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    if (minutes < 1) return "এইমাত্র"
    if (minutes < 60) return "${toBanglaDigits(minutes.toInt())} মিনিট আগে"
    if (hours < 24) return "${toBanglaDigits(hours.toInt())} ঘণ্টা আগে"

    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val period = when (hourOfDay) {
        in 4..11 -> "সকাল"
        in 12..14 -> "দুপুর"
        in 15..17 -> "বিকাল"
        in 18..19 -> "সন্ধ্যা"
        else -> "রাত"
    }

    val hour12 = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
    val minuteStr = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MINUTE))
    val timeFormatted = "$period ${toBanglaDigits(hour12)}:${toBanglaDigits(minuteStr)}"

    return if (days < 2) {
        "গতকাল $timeFormatted"
    } else {
        val monthNames = arrayOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val month = monthNames[cal.get(java.util.Calendar.MONTH)]
        "${toBanglaDigits(day)} $month, $timeFormatted"
    }
}

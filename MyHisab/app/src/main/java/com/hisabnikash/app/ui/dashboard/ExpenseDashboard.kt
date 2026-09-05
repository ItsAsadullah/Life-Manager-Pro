package com.hisabnikash.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.data.local.TransactionEntity
import com.hisabnikash.app.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getTimestampFromBanglaDate(dateStr: String): Long {
    return try {
        val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val englishDigits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var english = dateStr
        banglaDigits.forEachIndexed { i, c -> english = english.replace(c, englishDigits[i]) }
        val format = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
        format.parse(english)?.time ?: 0L
    } catch (e: Exception) { 0L }
}

fun monthStrInDate(monthStr: String, date: String): Boolean = monthStr in date

@Composable
fun ExpenseDashboard(
    transactions: List<TransactionEntity>,
    categories: List<String>,
    quickEntries: List<com.hisabnikash.app.data.local.QuickEntryEntity>,
    wallets: List<com.hisabnikash.app.data.local.WalletEntity> = emptyList(),
    onQuickEntryClick: (com.hisabnikash.app.data.local.QuickEntryEntity) -> Unit,
    onManageCategories: () -> Unit,
    onManageQuickEntries: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    viewModel: TransactionViewModel? = null
) {
    val haptic = LocalHapticFeedback.current
    
    var searchQuery by remember { mutableStateOf("") }
    var transactionFilter by remember { mutableIntStateOf(0) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("ExpenseDashboardPrefs", android.content.Context.MODE_PRIVATE) }
    
    var showPercentageBar by remember { mutableStateOf(sharedPrefs.getBoolean("showPercentageBar", true)) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(sharedPrefs.getBoolean("showSearchBar", true)) }
    var showGraphSheet by remember { mutableStateOf(false) }
    
    var showCategoryFilterRow by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var isCatSearchExpanded by remember { mutableStateOf(false) }

    var showAccountFilterRow by remember { mutableStateOf(false) }
    var selectedAccountFilter by remember { mutableStateOf<String?>(null) }

    var showCalendarFilterMenu by remember { mutableStateOf(false) }
    var activePicker by remember { mutableStateOf<String?>(null) } 
    var activeFilterType by remember { mutableStateOf<String?>(null) } 
    var activeFilterValue by remember { mutableStateOf<Any?>(null) } 

    val currentMonthStr = SimpleDateFormat("MMM", Locale("bn", "BD")).format(Calendar.getInstance().time)

    val displayedTransactions = transactions.filter { tx ->
        val matchesSearch = tx.title.contains(searchQuery, ignoreCase = true) || searchQuery.isBlank()
        val matchesType = when (transactionFilter) { 1 -> tx.isIncome; 2 -> !tx.isIncome; else -> true }
        val matchesCategory = selectedCategoryFilter == null || tx.category == selectedCategoryFilter
        
        val matchesAccount = selectedAccountFilter == null || 
            tx.paymentMethod == selectedAccountFilter || 
            (selectedAccountFilter == "নগদ ক্যাশ" && (tx.paymentMethod.isNullOrBlank() || tx.paymentMethod == "ক্যাশ" || tx.paymentMethod == "নগদ ক্যাশ")) ||
            (tx.paymentMethod.isNullOrBlank() && wallets.find { it.name == selectedAccountFilter }?.isDefault == true)

        val matchesCalendar = when(activeFilterType) {
            "date" -> {
                val targetTime = activeFilterValue as? Long ?: 0L
                val targetStr = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(targetTime)
                val banglaDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
                val targetBangla = targetStr.map { c ->
                    if (c in '0'..'9') banglaDigits[c - '0'] else c
                }.joinToString("")
                tx.date.replace(" ", "") == targetBangla.replace(" ", "")
            }
            "month" -> {
                @Suppress("UNCHECKED_CAST")
                val pair = activeFilterValue as? Pair<Int, Int> ?: Pair(-1, -1)
                val (monthIndex, year) = pair
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, monthIndex)
                val monthStr: String = SimpleDateFormat("MMM", Locale("bn", "BD")).format(cal.time)
                val yearStr: String = year.toBanglaString()
                monthStr in tx.date && yearStr in tx.date
            }
            "year" -> {
                val year = activeFilterValue as? Int ?: -1
                val yearStr: String = year.toBanglaString()
                yearStr in tx.date
            }
            "custom" -> {
                @Suppress("UNCHECKED_CAST")
                val pair = activeFilterValue as? Pair<Long, Long> ?: Pair(0L, Long.MAX_VALUE)
                val (start, end) = pair
                val txTime = getTimestampFromBanglaDate(tx.date)
                txTime in start..end
            }
            else -> true
        }
        matchesSearch && matchesType && matchesCategory && matchesCalendar && matchesAccount
    }
    val groupedTransactions = displayedTransactions.groupBy { it.date }

    val totalIncome = displayedTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = displayedTransactions.filter { !it.isIncome }.sumOf { it.amount }
    
    val currentMonthIncome = transactions.filter { it.isIncome && monthStrInDate(currentMonthStr, it.date) }.sumOf { it.amount }
    val currentMonthExpense = transactions.filter { !it.isIncome && monthStrInDate(currentMonthStr, it.date) }.sumOf { it.amount }
    val lastMonthIncome = transactions.filter { it.isIncome && !monthStrInDate(currentMonthStr, it.date) }.sumOf { it.amount }
    val lastMonthExpense = transactions.filter { !it.isIncome && !monthStrInDate(currentMonthStr, it.date) }.sumOf { it.amount }
    
    val incomePercent = if (lastMonthIncome > 0) ((currentMonthIncome - lastMonthIncome) / lastMonthIncome) * 100 else if (currentMonthIncome > 0) 100.0 else 0.0
    val expensePercent = if (lastMonthExpense > 0) ((currentMonthExpense - lastMonthExpense) / lastMonthExpense) * 100 else if (currentMonthExpense > 0) 100.0 else 0.0

    val todayStr = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(Calendar.getInstance().time)
    val banglaDigitsForToday = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val todayBangla = todayStr.map { c -> if (c in '0'..'9') banglaDigitsForToday[c - '0'] else c }.joinToString("")
    
    val todayIncome = displayedTransactions.filter { it.isIncome && it.date.replace(" ", "") == todayBangla.replace(" ", "") }.sumOf { it.amount }
    val todayExpense = displayedTransactions.filter { !it.isIncome && it.date.replace(" ", "") == todayBangla.replace(" ", "") }.sumOf { it.amount }

    val displayMonthCal = Calendar.getInstance()
    if (activeFilterType == "month") {
        @Suppress("UNCHECKED_CAST")
        val pair = activeFilterValue as? Pair<Int, Int> ?: Pair(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR))
        displayMonthCal.set(Calendar.MONTH, pair.first)
        displayMonthCal.set(Calendar.YEAR, pair.second)
    } else if (activeFilterType == "date") {
        val targetTime = activeFilterValue as? Long ?: Calendar.getInstance().timeInMillis
        displayMonthCal.timeInMillis = targetTime
    }
    
    val displayMonthStr: String = SimpleDateFormat("MMM", Locale("bn", "BD")).format(displayMonthCal.time)
    val displayMonthLabel = SimpleDateFormat("MMMM", Locale("bn", "BD")).format(displayMonthCal.time)
    val displayYearStr: String = displayMonthCal.get(Calendar.YEAR).toBanglaString()

    val monthIncome = displayedTransactions.filter { it.isIncome && displayMonthStr in it.date && displayYearStr in it.date }.sumOf { it.amount }
    val monthExpense = displayedTransactions.filter { !it.isIncome && displayMonthStr in it.date && displayYearStr in it.date }.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = showPercentageBar, enter = expandVertically(), exit = shrinkVertically()) {
            Column { MonthlyPercentageBar(incomePercent, expensePercent, onClose = { showPercentageBar = false }); Spacer(modifier = Modifier.height(12.dp)) }
        }

        val totalOpeningBalance = remember(wallets) { wallets.sumOf { it.balance } }
        GlassSummarySection(
            todayIncome = todayIncome, todayExpense = todayExpense,
            monthIncome = monthIncome, monthExpense = monthExpense,
            totalIncome = totalIncome, totalExpense = totalExpense,
            monthLabel = displayMonthLabel,
            totalOpeningBalance = totalOpeningBalance
        )

        // Quick Entry Row
        if (quickEntries.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickEntries) { entry ->
                    val textColor = if (entry.isIncome) Color(0xFF34C759) else Color(0xFFFF3B30)
                    val bgColor = if (entry.isIncome) Color(0xFF34C759).copy(alpha = 0.15f) else Color(0xFFFF3B30).copy(alpha = 0.15f)
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .clickable {
                                onQuickEntryClick(entry)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${entry.title} (${entry.amount.toBanglaString()})",
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape)
                            .clickable { onManageQuickEntries() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Quick Entries", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), glassAlpha = 0.12f) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompactSegmentedControl(items = listOf("সব", "আয়", "ব্যয়"), selectedIndex = transactionFilter, onItemSelected = { transactionFilter = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) })
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Account Filter",
                            tint = if (showAccountFilterRow || selectedAccountFilter != null) BalanceBlue else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).clickable {
                                showAccountFilterRow = !showAccountFilterRow
                                if (showAccountFilterRow) showCategoryFilterRow = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = if (activeFilterType != null) BalanceBlue else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).clickable { showCalendarFilterMenu = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
                        Icon(Icons.Default.Category, contentDescription = "Category", tint = if (showCategoryFilterRow || selectedCategoryFilter != null) BalanceBlue else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).clickable { showCategoryFilterRow = !showCategoryFilterRow; if (showCategoryFilterRow) showAccountFilterRow = false; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) })
                        Box {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp).clickable { showMenu = true })
                            CustomDropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                showSearchBar = showSearchBar,
                                showPercentageBar = showPercentageBar,
                                onToggleSearchBar = { 
                                    showSearchBar = it 
                                    sharedPrefs.edit().putBoolean("showSearchBar", it).apply()
                                },
                                onTogglePercentageBar = { 
                                    showPercentageBar = it 
                                    sharedPrefs.edit().putBoolean("showPercentageBar", it).apply()
                                },
                                onShowGraph = { showMenu = false; showGraphSheet = true },
                                onManageQuickEntries = { showMenu = false; onManageQuickEntries() }
                            )
                        }
                    }
                }
                
                AnimatedVisibility(visible = showSearchBar && !showCategoryFilterRow && !showAccountFilterRow) {
                    Column { Spacer(modifier = Modifier.height(8.dp)); CompactSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }) }
                }

                // Account Filter Row
                AnimatedVisibility(visible = showAccountFilterRow) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val availableWallets = if (wallets.isEmpty()) {
                                listOf("নগদ ক্যাশ")
                            } else {
                                wallets.map { it.name }
                            }
                            items(availableWallets) { wName ->
                                val isSelected = selectedAccountFilter == wName
                                val count = transactions.count { 
                                    it.paymentMethod == wName || 
                                    (wName == "নগদ ক্যাশ" && (it.paymentMethod.isNullOrBlank() || it.paymentMethod == "ক্যাশ" || it.paymentMethod == "নগদ ক্যাশ")) ||
                                    (it.paymentMethod.isNullOrBlank() && wallets.find { w -> w.name == wName }?.isDefault == true)
                                }
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) BalanceBlue else Color.Transparent)
                                        .border(1.dp, if (isSelected) Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .clickable { 
                                            selectedAccountFilter = if (isSelected) null else wName
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) 
                                        }
                                        .padding(horizontal = 10.dp), 
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$wName ($count)", 
                                        color = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface, 
                                        fontSize = 12.sp, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Filter Row
                AnimatedVisibility(visible = showCategoryFilterRow) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        
                        if (isCatSearchExpanded) {
                            Box(modifier = Modifier.width(140.dp).fillMaxHeight().border(1.dp, BalanceBlue, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicTextField(value = categorySearchQuery, onValueChange = { categorySearchQuery = it }, textStyle = TextStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp), singleLine = true, modifier = Modifier.weight(1f), decorationBox = { inner -> if (categorySearchQuery.isEmpty()) Text("সার্চ...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); inner() })
                                    Icon(Icons.Default.Close, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp).clickable { isCatSearchExpanded = false; categorySearchQuery = "" })
                                }
                            }
                        } else {
                            Box(modifier = Modifier.size(28.dp).border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape).clickable { isCatSearchExpanded = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Search, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            items(categories.filter { it.contains(categorySearchQuery, ignoreCase = true) }) { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                val count = transactions.count { it.category == cat }
                                Box(modifier = Modifier.height(28.dp).clip(RoundedCornerShape(20.dp)).background(if (isSelected) BalanceBlue else Color.Transparent).border(1.dp, if (isSelected) Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).clickable { selectedCategoryFilter = if (isSelected) null else cat; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("$cat $count", color = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                            item {
                                Box(modifier = Modifier.size(36.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape).clickable { onManageCategories() }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Settings, contentDescription = "Manage", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(visible = selectedAccountFilter != null) {
            val accIncome = displayedTransactions.filter { it.isIncome }.sumOf { it.amount }
            val accExpense = displayedTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val count = displayedTransactions.size
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selectedAccountFilter ?: "", color = BalanceBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("${count.toBanglaString()} টি", color = BalanceBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(12.dp))
                Text("আয়: ${accIncome.toBanglaString()}", color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(12.dp))
                Text("ব্যয়: ${accExpense.toBanglaString()}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        AnimatedVisibility(visible = selectedCategoryFilter != null) {
            val catIncome = displayedTransactions.filter { it.isIncome && it.category == selectedCategoryFilter }.sumOf { it.amount }
            val catExpense = displayedTransactions.filter { !it.isIncome && it.category == selectedCategoryFilter }.sumOf { it.amount }
            val count = displayedTransactions.count { it.category == selectedCategoryFilter }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(Color.White.copy(0.05f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selectedCategoryFilter ?: "", color = BalanceBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("${count.toBanglaString()} টি", color = BalanceBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(12.dp))
                Text("আয়: ${catIncome.toBanglaString()}", color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(12.dp))
                Text("ব্যয়: ${catExpense.toBanglaString()}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
            if (groupedTransactions.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text("কোনো লেনদেন পাওয়া যায়নি", color = TextWhiteSecondary) } }
            } else {
                groupedTransactions.forEach { (date, dailyTransactions) ->
                    val dailyIncome = dailyTransactions.filter { it.isIncome }.sumOf { it.amount }
                    val dailyExpense = dailyTransactions.filter { !it.isIncome }.sumOf { it.amount }
                    item {
                        TransactionGroupHeader(date = date, income = dailyIncome.toBanglaString(), expense = dailyExpense.toBanglaString())
                        GlassTransactionGroup(transactions = dailyTransactions, onEdit = onEditTransaction, onDelete = onDeleteTransaction, viewModel = viewModel)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showCalendarFilterMenu) {
        CalendarFilterDialog(
            isActive = activeFilterType != null,
            onDismiss = { showCalendarFilterMenu = false },
            onOptionSelected = { option ->
                showCalendarFilterMenu = false
                if (option == null) { activeFilterType = null; activeFilterValue = null } else { activePicker = option }
            }
        )
    }

    when (activePicker) {
        "date" -> IOSDatePickerDialog(onDismiss = { activePicker = null }, onDateSelected = { time -> if (time != null) { activeFilterType = "date"; activeFilterValue = time }; activePicker = null })
        "month" -> IOSMonthPickerDialog(onDismiss = { activePicker = null }, onMonthSelected = { m, y -> activeFilterType = "month"; activeFilterValue = Pair(m, y); activePicker = null })
        "year" -> IOSYearPickerDialog(onDismiss = { activePicker = null }, onYearSelected = { y -> activeFilterType = "year"; activeFilterValue = y; activePicker = null })
        "custom" -> IOSDateRangePickerDialog(onDismiss = { activePicker = null }, onRangeSelected = { s, e -> if (s != null && e != null) { activeFilterType = "custom"; activeFilterValue = Pair(s, e) }; activePicker = null })
    }

    if (showGraphSheet) {
        GraphBottomSheet(
            transactions = transactions,
            onDismiss = { showGraphSheet = false }
        )
    }
}

// ... [CustomDropdownMenu, MonthlyPercentageBar, CompactSegmentedControl, CompactSearchBar, GlassSummarySection, TransactionGroupHeader, GlassTransactionGroup, TransactionRow আগের মতোই থাকবে]
@Composable
fun CustomDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showSearchBar: Boolean,
    showPercentageBar: Boolean,
    onToggleSearchBar: (Boolean) -> Unit,
    onTogglePercentageBar: (Boolean) -> Unit,
    onShowGraph: () -> Unit = {},
    onManageQuickEntries: () -> Unit = {}
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp)),
        colorScheme = MaterialTheme.colorScheme.copy(surface = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = onDismissRequest, 
            modifier = Modifier.width(220.dp)
        ) {
            CustomDropdownMenuItem(icon = Icons.Default.PieChart, text = "গ্রাফ দেখুন", onClick = onShowGraph)
            CustomDropdownMenuItem(icon = Icons.Default.Speed, text = "কুইক এন্ট্রি", onClick = onManageQuickEntries)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.1f))
            DropdownMenuCheckboxItem(text = "সার্চবার শো করুন", checked = showSearchBar, onCheckedChange = onToggleSearchBar)
            DropdownMenuCheckboxItem(text = "মাসিক পার্সেন্টেজ বার", checked = showPercentageBar, onCheckedChange = onTogglePercentageBar)
        }
    }
}

@Composable
fun CustomDropdownMenuItem(icon: ImageVector, text: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(icon, contentDescription = null, tint = TextWhiteSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DropdownMenuCheckboxItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = text, color = Color.White, fontSize = 15.sp)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = BalanceBlue, checkmarkColor = Color.White, uncheckedColor = TextWhiteSecondary))
    }
}

@Composable
fun MonthlyPercentageBar(incomePercent: Double, expensePercent: Double, onClose: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), glassAlpha = 0.15f) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("“আয়” গত মাস থেকে", fontSize = 11.sp, color = TextWhiteSecondary)
                }
                Text("${incomePercent.toBanglaString()}% বেশি", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
            }
            Box(modifier = Modifier.height(30.dp).width(1.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("“ব্যয়” গত মাস থেকে", fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${expensePercent.toBanglaString()}% বেশি", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
            }
            Icon(Icons.Default.Close, contentDescription = "Close", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).clickable { onClose() })
        }
    }
}

@Composable
fun CompactSegmentedControl(items: List<String>, selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).padding(2.dp)) {
        items.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(6.dp)).background(if (isSelected) BalanceBlue else Color.Transparent).clickable { onItemSelected(index) }, contentAlignment = Alignment.Center) {
                Text(text = title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CompactSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(value = query, onValueChange = onQueryChange, textStyle = TextStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp), singleLine = true, modifier = Modifier.fillMaxWidth(), decorationBox = { innerTextField ->
                if (query.isEmpty()) Text("সার্চ...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                innerTextField()
            })
        }
    }
}

@Composable
fun GlassSummarySection(
    todayIncome: Double, todayExpense: Double,
    monthIncome: Double, monthExpense: Double,
    totalIncome: Double, totalExpense: Double,
    monthLabel: String,
    totalOpeningBalance: Double = 0.0
) {
    var selectedSegment by remember { mutableIntStateOf(0) }

    val currentIncome = when (selectedSegment) {
        0 -> todayIncome
        1 -> monthIncome
        else -> totalIncome
    }
    
    val currentExpense = when (selectedSegment) {
        0 -> todayExpense
        1 -> monthExpense
        else -> totalExpense
    }
    
    val currentBalance = if (selectedSegment == 2 && totalOpeningBalance > 0.0) {
        totalOpeningBalance + currentIncome - currentExpense
    } else {
        currentIncome - currentExpense
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), glassAlpha = 0.12f) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            CompactSegmentedControl(items = listOf("আজ", monthLabel, "মোট"), selectedIndex = selectedSegment, onItemSelected = { selectedSegment = it })
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("আয়", "৳ ${currentIncome.toBanglaString()}", IncomeGreen)
                SummaryItem("ব্যয়", "৳ ${currentExpense.toBanglaString()}", ExpenseRed)
                SummaryItem(
                    title = if (selectedSegment == 2 && totalOpeningBalance > 0.0) "মোট সম্পদ" else "ব্যালেন্স",
                    amount = "৳ ${currentBalance.toBanglaString()}",
                    color = BalanceBlue
                )
            }
        }
    }
}

@Composable
fun SummaryItem(title: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = color.copy(alpha = 0.8f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(amount, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionGroupHeader(date: String, income: String, expense: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(date, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            Text("মোট ", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Text("৳$income", fontSize = 12.sp, color = IncomeGreen)
            Text("  ৳$expense", fontSize = 12.sp, color = ExpenseRed)
        }
    }
}

@Composable
fun GlassTransactionGroup(
    transactions: List<TransactionEntity>,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    viewModel: TransactionViewModel? = null
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), glassAlpha = 0.1f) {
        Column(modifier = Modifier.fillMaxWidth()) {
            transactions.forEachIndexed { index, tx ->
                TransactionRow(
                    transaction = tx,
                    onEdit = { onEdit(tx) },
                    onDelete = { onDelete(tx) },
                    viewModel = viewModel
                )
                if (index < transactions.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: TransactionViewModel? = null
) {
    val sign = if (transaction.isIncome) "+" else "-"
    val iconColor = if (transaction.isIncome) IncomeGreen else ExpenseRed
    var expandedMenu by remember { mutableStateOf(false) }
    var showMarketBreakdown by remember { mutableStateOf(false) }
    
    val rawMethod = transaction.paymentMethod
    val accountName = if (rawMethod.isNullOrBlank() || rawMethod == "ক্যাশ") "নগদ ক্যাশ" else rawMethod
    val isIncome = transaction.isIncome
    val badgeBg = if (isIncome) Color(0xFF34C759).copy(alpha = 0.12f) else Color(0xFFFF3B30).copy(alpha = 0.10f)
    val badgeBorder = if (isIncome) Color(0xFF34C759).copy(alpha = 0.25f) else Color(0xFFFF3B30).copy(alpha = 0.25f)
    val badgeColor = if (isIncome) Color(0xFF34C759) else Color(0xFFFF453A)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (transaction.shoppingListId != null) {
                    showMarketBreakdown = true
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)).border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
            Text(sign, color = iconColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // লাইন ১: শিরোনাম
            Text(
                text = transaction.title,
                fontSize = 15.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            
            // লাইন ২: ওয়ালেট ব্যাজ + ক্যাটাগরি ও সময় (এক লাইনে কমপ্যাক্ট)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // কমপ্যাক্ট ওয়ালেট ইন্ডিকেটর
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .border(0.5.dp, badgeBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = accountName,
                        fontSize = 10.5.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // ক্যাটাগরি ও সময় (ক্যাটাগরি ফাঁকা বা টাইটেলের অনুরূপ হলে ডট ছাড়া শুধু সময় প্রদর্শিত হবে)
                val cleanCat = transaction.category.trim()
                val metaText = if (cleanCat.isNotBlank() && cleanCat != transaction.title.trim()) {
                    "$cleanCat • ${transaction.time}"
                } else {
                    transaction.time
                }
                if (metaText.isNotBlank()) {
                    Text(
                        text = metaText,
                        fontSize = 11.5.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (transaction.shoppingListId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0A84FF).copy(alpha = 0.15f))
                            .clickable { showMarketBreakdown = true }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ফর্দ", fontSize = 9.5.sp, color = Color(0xFF7CEBFF), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text(text = "$sign ৳${transaction.amount.toBanglaString()}", fontSize = 17.sp, color = iconColor, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp).clickable { expandedMenu = true }.padding(2.dp))
            DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }, modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))) {
                if (transaction.shoppingListId != null) {
                    DropdownMenuItem(
                        text = { Text("বাজার ফর্দ দেখুন", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF0A84FF)) },
                        onClick = { expandedMenu = false; showMarketBreakdown = true }
                    )
                }
                DropdownMenuItem(text = { Text("এডিট", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) }, leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, onClick = { expandedMenu = false; onEdit() })
                DropdownMenuItem(text = { Text("ডিলিট", color = ExpenseRed, fontSize = 16.sp) }, leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ExpenseRed) }, onClick = { expandedMenu = false; onDelete() })
            }
        }
    }

    if (showMarketBreakdown && transaction.shoppingListId != null && viewModel != null) {
        MarketBreakdownDialog(
            shoppingListId = transaction.shoppingListId,
            viewModel = viewModel,
            onDismiss = { showMarketBreakdown = false }
        )
    }
}

@Composable
fun MarketBreakdownDialog(
    shoppingListId: String,
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit
) {
    val items by viewModel.getMarketItems(shoppingListId).collectAsState(initial = emptyList())
    val marketLists by viewModel.allMarketLists.collectAsState()
    val list = marketLists.find { it.id == shoppingListId }

    val purchasedItems = items.filter { it.isPurchased }
    val totalActual = purchasedItems.sumOf { (if (it.actualPrice > 0.0) it.actualPrice else it.estimatedPrice) * it.quantity.toEnglishDouble() }

    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .clickable(interactionSource = interactionSource, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = {})) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.12f else 0.04f
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(list?.title ?: "বাজারের ফর্দ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                                    val shopText = if (list?.shopName?.isNotBlank() == true) "🏪 ${list.shopName} • " else ""
                                    Text("$shopText${list?.date ?: ""}", fontSize = 12.sp, color = subtextColor)
                                }
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

                        Spacer(modifier = Modifier.height(14.dp))

                        if (purchasedItems.isEmpty()) {
                            Text("এই বাজারে কোনো কেনা পণ্যের রেকর্ড পাওয়া যায়নি।", fontSize = 14.sp, color = subtextColor)
                        } else {
                            Text(
                                "কেনা পণ্যের বিবরণ (${purchasedItems.size.toLong().toBanglaString()} টি):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(purchasedItems, key = { it.id }) { item ->
                                    val qty = item.quantity.toEnglishDouble()
                                    val price = if (item.actualPrice > 0.0) item.actualPrice else item.estimatedPrice
                                    val itemTotal = price * (if (qty > 0.0) qty else 1.0)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                            Text("${item.quantity} ${item.unit} @ ৳ ${price.toBanglaString()}", fontSize = 11.sp, color = subtextColor)
                                            if (item.note.isNotBlank()) {
                                                Text("📝 ${item.note}", fontSize = 10.sp, color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF))
                                            }
                                        }
                                        Text("৳ ${itemTotal.toBanglaString()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("মোট আসল খরচ:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                Text("৳ ${totalActual.toBanglaString()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF30D158))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("ঠিক আছে", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}



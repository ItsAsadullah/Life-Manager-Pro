package com.hisabnikash.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.data.local.BudgetEntity
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.theme.BalanceBlue
import com.hisabnikash.app.ui.theme.ExpenseRed
import com.hisabnikash.app.ui.theme.IncomeGreen
import com.hisabnikash.app.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: TransactionViewModel,
    categories: List<String>,
    onManageCategories: () -> Unit = {},
    showAddExternally: Boolean = false,
    onAddDismissed: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    val budgets by viewModel.allBudgets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetEntity?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(showAddExternally) {
        if (showAddExternally) {
            budgetToEdit = null
            showAddDialog = true
            onAddDismissed()
        }
    }

    val currentCal = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }

    val selectedCal = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.YEAR, selectedYear)
        }
    }

    val displayMonthShort = SimpleDateFormat("MMM", Locale("bn", "BD")).format(selectedCal.time)
    val displayMonthName = SimpleDateFormat("MMMM", Locale("bn", "BD")).format(selectedCal.time)
    val displayYearBangla = selectedYear.toBanglaString()
    val currentMonthYearKey = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)

    // Filter budgets for the selected month/year
    val filteredBudgets = budgets.filter { it.monthYear == currentMonthYearKey }

    // Calculate total expenses and income for the selected month
    val monthTransactions = transactions.filter {
        displayMonthShort in it.date && displayYearBangla in it.date
    }
    val totalExpenses = monthTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val totalIncome = monthTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalBudgetLimit = filteredBudgets.sumOf { it.monthlyLimit }

    val overallPercentage = if (totalBudgetLimit > 0) {
        ((totalExpenses / totalBudgetLimit) * 100).toInt()
    } else {
        0
    }
    val isOverBudget = totalBudgetLimit > 0 && totalExpenses > totalBudgetLimit
    val overallProgressFraction = if (totalBudgetLimit > 0) {
        (totalExpenses / totalBudgetLimit).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    // Direct Column matching ExpenseDashboard and LendBorrowScreen top alignment
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ==========================================
        // TOP HEADER CARD (বাজেট ম্যানেজমেন্ট)
        // ==========================================
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            glassAlpha = 0.12f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header Row: Title + Month Picker Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "বাজেট ম্যানেজমেন্ট",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showMonthPicker = true
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$displayMonthName $displayYearBangla",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Month Picker",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar with Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(overallProgressFraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isOverBudget) ExpenseRed else PrimaryBlue)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${overallPercentage.toLong().toBanglaString()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) ExpenseRed else PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Stats: মোট খরচ vs মোট বাজেট
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মোট খরচ: ৳${totalExpenses.toBanglaString()}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "মোট বাজেট: ৳${totalBudgetLimit.toBanglaString()}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // CATEGORY BUDGETS LIST
        // ==========================================
        if (filteredBudgets.isEmpty()) {
            EmptyStateContent(
                icon = Icons.Default.Timeline,
                title = "কোন বাজেট সেট করা নেই",
                subtitle = "+ বাটনে ক্লিক করে $displayMonthName মাসের বাজেট যোগ করুন"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBudgets, key = { it.id }) { budget ->
                    CategoryBudgetCard(
                        budget = budget,
                        transactions = monthTransactions,
                        displayMonthShort = displayMonthShort,
                        displayYearBangla = displayYearBangla,
                        isDark = isDark,
                        onEdit = {
                            budgetToEdit = budget
                            showAddDialog = true
                        },
                        onDelete = {
                            viewModel.deleteBudget(budget)
                        }
                    )
                }
            }
        }
    }

    // ==========================================
    // CUSTOM iOS 3D WHEEL MONTH PICKER
    // ==========================================
    if (showMonthPicker) {
        IOSMonthPickerDialog(
            onDismiss = { showMonthPicker = false },
            onMonthSelected = { m, y ->
                selectedMonth = m
                selectedYear = y
                showMonthPicker = false
            }
        )
    }

    // ==========================================
    // ADD / EDIT BUDGET DIALOG
    // ==========================================
    if (showAddDialog) {
        AddOrEditBudgetDialog(
            budgetToEdit = budgetToEdit,
            categories = categories,
            monthYearKey = currentMonthYearKey,
            totalMonthlyIncome = totalIncome,
            onManageCategories = onManageCategories,
            onDismiss = {
                showAddDialog = false
                budgetToEdit = null
            },
            onSave = { category, limitAmount ->
                if (budgetToEdit != null) {
                    viewModel.updateBudget(
                        budgetToEdit!!.copy(
                            category = category,
                            monthlyLimit = limitAmount,
                            monthYear = currentMonthYearKey
                        )
                    )
                } else {
                    viewModel.insertBudget(
                        BudgetEntity(
                            category = category,
                            monthlyLimit = limitAmount,
                            monthYear = currentMonthYearKey
                        )
                    )
                }
                showAddDialog = false
                budgetToEdit = null
            }
        )
    }
}

/**
 * Individual Category Budget Card (Matching Screenshot 1 & 5)
 */
@Composable
fun CategoryBudgetCard(
    budget: BudgetEntity,
    transactions: List<com.hisabnikash.app.data.local.TransactionEntity>,
    displayMonthShort: String,
    displayYearBangla: String,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val catExpenses = transactions.filter {
        !it.isIncome && it.category == budget.category && displayMonthShort in it.date && displayYearBangla in it.date
    }.sumOf { it.amount }

    val catPercentage = if (budget.monthlyLimit > 0) {
        ((catExpenses / budget.monthlyLimit) * 100).toInt()
    } else {
        0
    }
    val isOver = budget.monthlyLimit > 0 && catExpenses > budget.monthlyLimit
    val progressFraction = if (budget.monthlyLimit > 0) {
        (catExpenses / budget.monthlyLimit).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    val remaining = maxOf(0.0, budget.monthlyLimit - catExpenses)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        glassAlpha = 0.12f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category Name (Limit) + Options Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = budget.category,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(৳${budget.monthlyLimit.toBanglaString()})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showMenu = true
                            }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        DropdownMenuItem(
                            text = { Text("এডিট", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ডিলিট", color = ExpenseRed, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ExpenseRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isOver) ExpenseRed else PrimaryBlue)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${catPercentage.toLong().toBanglaString()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) ExpenseRed else PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Two Pill Chips (ব্যয় & বাকি)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Pill (ব্যয়)
                val expensePillBg = if (isDark) {
                    ExpenseRed.copy(alpha = 0.15f)
                } else {
                    Color(0xFFFFEEEE)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(expensePillBg)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ব্যয়",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "৳${catExpenses.toBanglaString()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                // Right Pill (বাকি)
                val remainingPillBg = if (isDark) {
                    IncomeGreen.copy(alpha = 0.15f)
                } else {
                    Color(0xFFEAF8EE)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(remainingPillBg)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বাকি",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "৳${remaining.toBanglaString()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add or Edit Budget Modal matching Screenshot 3 & 4
 */
@Composable
fun AddOrEditBudgetDialog(
    budgetToEdit: BudgetEntity?,
    categories: List<String>,
    monthYearKey: String,
    totalMonthlyIncome: Double,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (category: String, limitAmount: Double) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val view = LocalView.current
    var showCalculator by remember { mutableStateOf(false) }

    fun dismissKeyboard() {
        showCalculator = false
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            imm?.hideSoftInputFromWindow(view.applicationWindowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 0 = পার্সেন্টেজ (%), 1 = ফিক্সড টাকা (৳)
    var selectedMode by remember { mutableIntStateOf(1) }
    var percentageText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf(budgetToEdit?.monthlyLimit?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var selectedCategory by remember { mutableStateOf(budgetToEdit?.category ?: (categories.firstOrNull() ?: "খাবার")) }
    var showCategoryBottomSheet by remember { mutableStateOf(false) }

    val percentageInteractionSource = remember { MutableInteractionSource() }
    val percentageIsPressed by percentageInteractionSource.collectIsPressedAsState()
    LaunchedEffect(percentageIsPressed) {
        if (percentageIsPressed) {
            showCalculator = true
            keyboardController?.hide()
        }
    }

    val amountInteractionSource = remember { MutableInteractionSource() }
    val amountIsPressed by amountInteractionSource.collectIsPressedAsState()
    LaunchedEffect(amountIsPressed) {
        if (amountIsPressed) {
            showCalculator = true
            keyboardController?.hide()
        }
    }

    val backdropColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.45f)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.98f) else Color.White

    Dialog(
        onDismissRequest = {
            dismissKeyboard()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        dismissKeyboard()
                        onDismiss()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            // Main Input Form Card (Screenshot 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .imePadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.1f else 0.05f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (budgetToEdit == null) "নতুন বাজেট যোগ করুন" else "বাজেট এডিট করুন",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Segmented Control (পার্সেন্টেজ (%) vs ফিক্সড টাকা (৳))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .padding(3.dp)
                        ) {
                            // Option 1: পার্সেন্টেজ (%)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedMode == 0) Color(0xFF007AFF) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMode = 0
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "পার্সেন্টেজ (%)",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedMode == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Option 2: ফিক্সড টাকা (৳)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedMode == 1) Color(0xFF007AFF) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMode = 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ফিক্সড টাকা (৳)",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedMode == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Input Field (Percentage or Amount)
                        if (selectedMode == 0) {
                            OutlinedTextField(
                                value = percentageText,
                                onValueChange = { percentageText = it.toEnglishDigits() },
                                label = { Text("পার্সেন্টেজ (%)") },
                                placeholder = { Text("০ থেকে ১০০", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                readOnly = true,
                                interactionSource = percentageInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            showCalculator = true
                                            keyboardController?.hide()
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            val pVal = percentageText.toEnglishDouble()
                            if (pVal > 0 && totalMonthlyIncome > 0) {
                                val calc = (totalMonthlyIncome * pVal / 100.0)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "মাসিক মোট আয়ের $pVal% = ৳${calc.toBanglaString()}",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it.toEnglishDigits() },
                                label = { Text("টাকার পরিমাণ (৳)") },
                                placeholder = { Text("টাকার পরিমাণ (৳)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                readOnly = true,
                                interactionSource = amountInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            showCalculator = true
                                            keyboardController?.hide()
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Selector Box (Screenshot 3)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dismissKeyboard()
                                    showCategoryBottomSheet = true
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Category",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = selectedCategory.ifBlank { "ক্যাটাগরি বেছে নিন" },
                                        fontSize = 15.sp,
                                        color = if (selectedCategory.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Action Buttons (বাতিল / অ্যাড)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                dismissKeyboard()
                                onDismiss()
                            }) {
                                Text("বাতিল", color = Color(0xFF007AFF), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = {
                                    dismissKeyboard()
                                    if (selectedCategory.isBlank() || selectedCategory == "ক্যাটাগরি বেছে নিন") {
                                        android.widget.Toast.makeText(context, "দয়া করে একটি ক্যাটাগরি বেছে নিন", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val finalLimit = if (selectedMode == 0) {
                                        val percent = percentageText.toEnglishDouble()
                                        if (totalMonthlyIncome > 0) {
                                            (totalMonthlyIncome * percent / 100.0)
                                        } else {
                                            percent
                                        }
                                    } else {
                                        amountText.toEnglishDouble()
                                    }

                                    if (finalLimit <= 0) {
                                        android.widget.Toast.makeText(context, "দয়া করে টাকার পরিমাণ লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    if (selectedCategory.isNotBlank() && finalLimit > 0) {
                                        onSave(selectedCategory, finalLimit)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(46.dp).width(115.dp)
                            ) {
                                Text(
                                    text = if (budgetToEdit == null) "অ্যাড" else "আপডেট",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (showCalculator) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CustomCalculatorKeyboard(
                                value = if (selectedMode == 0) percentageText else amountText,
                                onValueChange = { 
                                    val cleaned = it.toEnglishDigits()
                                    if (selectedMode == 0) percentageText = cleaned else amountText = cleaned 
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Category Picker Dialog (Screenshot 4)
    if (showCategoryBottomSheet) {
        CategoryPickerDialog(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { cat ->
                selectedCategory = cat
                showCategoryBottomSheet = false
            },
            onManageCategories = {
                showCategoryBottomSheet = false
                onManageCategories()
            },
            onDismiss = { showCategoryBottomSheet = false }
        )
    }
}

/**
 * Category Selector Bottom Popup matching User Screenshot
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerDialog(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val coroutineScope = rememberCoroutineScope()
    fun dismissWithAnimation() {
        if (!visible) return
        visible = false
        coroutineScope.launch {
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    val filteredCategories = categories.filter {
        searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true)
    }

    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.98f) else Color.White

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Dim background
                .background(Color.Black.copy(alpha = 0.65f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation() }
                ),
            contentAlignment = Alignment.BottomCenter // Align the sheet to the bottom
        ) {
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            val density = androidx.compose.ui.platform.LocalDensity.current
            val screenHeightPx = with(density) { screenHeight.roundToPx() }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { screenHeightPx },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { screenHeightPx },
                    animationSpec = tween(300)
                )
            ) {
                var dragOffset by remember { mutableFloatStateOf(0f) }

                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            val delta = available.y
                            if (delta < 0f && dragOffset > 0f) {
                                val newOffset = (dragOffset + delta).coerceAtLeast(0f)
                                val consumed = dragOffset - newOffset
                                dragOffset = newOffset
                                return Offset(0f, -consumed)
                            }
                            return Offset.Zero
                        }

                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            val delta = available.y
                            if (delta > 0f) {
                                dragOffset += delta
                                return Offset(0f, delta)
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            if (dragOffset > 250f || available.y > 1500f) {
                                dismissWithAnimation()
                                return Velocity(0f, available.y)
                            } else if (dragOffset > 0f) {
                                Animatable(dragOffset).animateTo(0f) {
                                    dragOffset = this.value
                                }
                                return Velocity(0f, available.y)
                            }
                            return Velocity.Zero
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .nestedScroll(nestedScrollConnection)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume clicks so they don't dismiss the dialog
                        ),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), // Rounded top only
                    color = cardBg,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp) // bottom padding for content
                    ) {
                        // Header area which is draggable to dismiss
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (dragOffset > 250f) {
                                                dismissWithAnimation()
                                            } else {
                                                coroutineScope.launch {
                                                    Animatable(dragOffset).animateTo(0f) { dragOffset = this.value }
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            coroutineScope.launch {
                                                Animatable(dragOffset).animateTo(0f) { dragOffset = this.value }
                                            }
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                                    }
                                }
                                .padding(horizontal = 22.dp)
                                .padding(top = 16.dp, bottom = 12.dp)
                        ) {
                            // Optional small handle pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                )
                            }
                            
                            // Header: Title centered + Gear & Close icons on right
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                text = "ক্যাটাগরি বেছে নিন",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        dismissWithAnimation()
                                        // Use a small delay before navigating or triggering manage so the animation can start
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(150)
                                            onManageCategories()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Manage Categories",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { dismissWithAnimation() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        }

                        // We extract the search bar and the list outside the draggable header
                        // to ensure verticalScroll works uninterrupted.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp)
                        ) {
                            // Search Bar: "ক্যাটাগরি সার্চ করুন..."
                            OutlinedTextField(
                                value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ক্যাটাগরি খুঁজুন...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Horizontal Categories Chips (FlowRow Layout)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 350.dp, max = 600.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    filteredCategories.forEach { cat ->
                                        val isSelected = selectedCategory == cat
                                        val chipBg = if (isSelected) {
                                            Color(0xFF007AFF)
                                        } else if (isDark) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        } else {
                                            Color(0xFFF2F2F7)
                                        }
                                        val textColor = if (isSelected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(chipBg)
                                                .border(
                                                    width = if (isSelected) 0.dp else 1.dp,
                                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onCategorySelected(cat)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }

                                if (filteredCategories.isEmpty() && searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                            .clickable {
                                                onCategorySelected(searchQuery.trim())
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+ \"$searchQuery\" ক্যাটাগরি হিসেবে ব্যবহার করুন",
                                            color = Color(0xFF007AFF),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
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
    }
}

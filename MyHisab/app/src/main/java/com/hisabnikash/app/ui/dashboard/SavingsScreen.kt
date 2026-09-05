package com.hisabnikash.app.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.data.local.SavingsGoalEntity
import com.hisabnikash.app.data.local.SavingsTransactionEntity
import com.hisabnikash.app.ui.components.GlassCard

// ─────────────────────────────────────────────────
//  MAIN SCREEN
// ─────────────────────────────────────────────────
@Composable
fun SavingsScreen(
    viewModel: TransactionViewModel,
    showAddGoalExternally: Boolean = false,
    onAddGoalDismissed: () -> Unit = {}
) {
    val goals by viewModel.allSavingsGoals.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    // Store only the goal ID to avoid recompose-loops from object reference changes
    var selectedGoalId by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0=চলমান, 1=সম্পূর্ণ

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Resolve the live goal from the current goals list (always fresh from DB)
    val selectedGoal = selectedGoalId?.let { id -> goals.find { it.id == id } }

    LaunchedEffect(showAddGoalExternally) {
        if (showAddGoalExternally) {
            goalToEdit = null
            showAddGoalDialog = true
            onAddGoalDismissed()
        }
    }

    // Simple if/else — no AnimatedContent → no unwanted transition
    if (selectedGoal == null) {
        // ── Goal List ──
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── চলমান / সম্পূর্ণ ট্যাব ───
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                glassAlpha = 0.05f
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SavingsTabButton(
                        label = "চলমান",
                        selected = selectedTab == 0,
                        activeColor = Color(0xFF30D158),
                        modifier = Modifier.weight(1f),
                        isLeft = true
                    ) { selectedTab = 0 }
                    SavingsTabButton(
                        label = "সম্পূর্ণ",
                        selected = selectedTab == 1,
                        activeColor = Color(0xFFFFD700),
                        modifier = Modifier.weight(1f),
                        isLeft = false
                    ) { selectedTab = 1 }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val activeGoals = goals.filter { it.currentAmount < it.targetAmount || it.targetAmount <= 0 }
            val completedGoals = goals.filter { it.targetAmount > 0 && it.currentAmount >= it.targetAmount }
            val displayGoals = if (selectedTab == 0) activeGoals else completedGoals

            if (displayGoals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateContent(
                        icon = Icons.Default.Savings,
                        title = if (selectedTab == 0) "কোনো চলমান সঞ্চয় নেই" else "কোনো সম্পূর্ণ সঞ্চয় নেই",
                        subtitle = if (selectedTab == 0) "+ বাটনে ট্যাপ করে নতুন সঞ্চয় শুরু করুন" else "সঞ্চয় সম্পূর্ণ হলে এখানে দেখাবে"
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayGoals, key = { it.id }) { goal ->
                        SavingsGoalCard(
                            goal = goal,
                            isDark = isDark,
                            isCompleted = selectedTab == 1,
                            onClick = { selectedGoalId = goal.id },
                            onEdit = { goalToEdit = goal; showAddGoalDialog = true },
                            onDelete = { viewModel.deleteSavingsGoal(goal) }
                        )
                    }
                }
            }
        }
    } else {
        androidx.activity.compose.BackHandler { selectedGoalId = null }
        // ── Goal Detail ──
        SavingsGoalDetailScreen(
            goal = selectedGoal,
            viewModel = viewModel,
            isDark = isDark,
            onBack = { selectedGoalId = null }
        )
    }

    // Add / Edit Goal Dialog
    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            isDark = isDark,
            existingGoal = goalToEdit,
            onDismiss = { showAddGoalDialog = false; goalToEdit = null },
            onSave = { title, target, targetDate ->
                if (goalToEdit != null) {
                    viewModel.updateSavingsGoal(
                        goalToEdit!!.copy(title = title, targetAmount = target, targetDate = targetDate)
                    )
                } else {
                    viewModel.insertSavingsGoal(
                        SavingsGoalEntity(title = title, targetAmount = target, targetDate = targetDate)
                    )
                }
                showAddGoalDialog = false
                goalToEdit = null
            }
        )
    }
}

// ─────────────────────────────────────────────────
//  TAB BUTTON
// ─────────────────────────────────────────────────
@Composable
fun SavingsTabButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    isLeft: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200), label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200), label = "tabText"
    )
    val shape = if (isLeft) RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    else RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)

    Box(
        modifier = modifier
            .background(bgColor, shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

// ─────────────────────────────────────────────────
//  GOAL CARD
// ─────────────────────────────────────────────────
@Composable
fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    isDark: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0)
        ((goal.currentAmount / goal.targetAmount) * 100).coerceAtMost(100.0) else 0.0
    val progressAnim by animateFloatAsState(
        targetValue = (progress / 100).toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "goalProgress"
    )
    val accentColor = if (isCompleted) Color(0xFFFFD700) else Color(0xFF30D158)
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        glassAlpha = if (isDark) 0.12f else 0.08f
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            goal.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (goal.targetDate.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(goal.targetDate, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✓ সম্পন্ন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("এডিট", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) },
                                onClick = { menuExpanded = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("ডিলিট", color = ExpenseRed, fontSize = 15.sp) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ExpenseRed) },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progressAnim).height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.linearGradient(
                                    if (isCompleted) listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                    else listOf(Color(0xFF30D158), Color(0xFF00C896))
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("${progress.toInt().toLong().toBanglaString()}%",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("জমা", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${goal.currentAmount.toBanglaString()}", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A84FF).copy(alpha = 0.10f)).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("বাকি", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${remaining.toBanglaString()}", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  GOAL DETAIL SCREEN
// ─────────────────────────────────────────────────
@Composable
fun SavingsGoalDetailScreen(
    goal: SavingsGoalEntity,
    viewModel: TransactionViewModel,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.getSavingsTransactionsForGoal(goal.id).collectAsState(initial = emptyList())

    // Compute balance from transactions (always fresh, no reactive DB sync needed)
    val totalDeposited = transactions.filter { it.isDeposit }.sumOf { it.amount }
    val totalWithdrawn = transactions.filter { !it.isDeposit }.sumOf { it.amount }
    val currentBalance = (totalDeposited - totalWithdrawn).coerceAtLeast(0.0)
    val progress = if (goal.targetAmount > 0) (currentBalance / goal.targetAmount * 100).coerceIn(0.0, 100.0) else 0.0
    val progressAnim by animateFloatAsState(
        targetValue = (progress / 100).toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "detailProgress"
    )
    val isCompleted = goal.targetAmount > 0 && currentBalance >= goal.targetAmount

    var showEntryDialog by remember { mutableStateOf(false) }
    var entryIsDeposit by remember { mutableStateOf(true) }
    var txToEdit by remember { mutableStateOf<SavingsTransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<SavingsTransactionEntity?>(null) }

    // Helper: update goal's stored currentAmount and show warning if newly completed
    fun syncGoalBalance(newBalance: Double, previousBalance: Double) {
        viewModel.updateSavingsGoal(goal.copy(currentAmount = newBalance))
        if (newBalance >= goal.targetAmount && previousBalance < goal.targetAmount && goal.targetAmount > 0) {
            Toast.makeText(context, "🎉 অভিনন্দন! \"${goal.title}\" এর লক্ষ্যমাত্রা পূরণ হয়েছে!", Toast.LENGTH_LONG).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header Card ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            glassAlpha = if (isDark) 0.15f else 0.10f
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                // Back + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (goal.targetDate.isNotBlank()) {
                            Text("লক্ষ্যের তারিখ: ${goal.targetDate}", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (isCompleted) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✓ সম্পন্ন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalStatChip("বর্তমান জমা", "৳${currentBalance.toBanglaString()}", Color(0xFF30D158), Modifier.weight(1f))
                    GoalStatChip("লক্ষ্যমাত্রা", "৳${goal.targetAmount.toBanglaString()}", Color(0xFF0A84FF), Modifier.weight(1f))
                    GoalStatChip(
                        "বাকি",
                        "৳${(goal.targetAmount - currentBalance).coerceAtLeast(0.0).toBanglaString()}",
                        Color(0xFFFF9F0A), Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(progressAnim).height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.linearGradient(
                                        if (isCompleted) listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                        else listOf(Color(0xFF30D158), Color(0xFF00C896))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${progress.toInt().toLong().toBanglaString()}%",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFFFFD700) else Color(0xFF30D158))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { entryIsDeposit = true; txToEdit = null; showEntryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("জমা দিন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { entryIsDeposit = false; txToEdit = null; showEntryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("উত্তোলন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Transaction List ──
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateContent(
                    icon = Icons.Default.Savings,
                    title = "কোনো লেনদেন নেই",
                    subtitle = "জমা বা উত্তোলন করে শুরু করুন"
                )
            }
        } else {
            Text("লেনদেনের ইতিহাস", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp))
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(transactions, key = { it.id }) { tx ->
                    SavingsTransactionCard(
                        tx = tx,
                        isDark = isDark,
                        onEdit = { txToEdit = tx; entryIsDeposit = tx.isDeposit; showEntryDialog = true },
                        onDelete = { txToDelete = tx }
                    )
                }
            }
        }
    }

    // Entry Dialog
    if (showEntryDialog) {
        SavingsEntryDialog(
            isDark = isDark,
            isDeposit = entryIsDeposit,
            existingTx = txToEdit,
            onDismiss = { showEntryDialog = false; txToEdit = null },
            onSave = { amount, note, date, isDeposit ->
                val prevBalance = currentBalance
                val newBalance: Double

                if (txToEdit != null) {
                    // Remove old tx effect, add new tx effect
                    val oldEffect = if (txToEdit!!.isDeposit) txToEdit!!.amount else -txToEdit!!.amount
                    val newEffect = if (isDeposit) amount else -amount
                    newBalance = (currentBalance - oldEffect + newEffect).coerceAtLeast(0.0)
                    viewModel.updateSavingsTransaction(
                        txToEdit!!.copy(amount = amount, isDeposit = isDeposit, note = note, date = date)
                    )
                } else {
                    val effect = if (isDeposit) amount else -amount
                    newBalance = (currentBalance + effect).coerceAtLeast(0.0)
                    viewModel.insertSavingsTransaction(
                        SavingsTransactionEntity(goalId = goal.id, amount = amount, isDeposit = isDeposit, note = note, date = date)
                    )
                }
                // Explicit goal update — no reactive loop
                syncGoalBalance(newBalance, prevBalance)
                showEntryDialog = false
                txToEdit = null
            }
        )
    }

    // Delete confirmation
    if (txToDelete != null) {
        SavingsDeleteConfirmDialog(
            isDark = isDark,
            onConfirm = {
                val deletedEffect = if (txToDelete!!.isDeposit) txToDelete!!.amount else -txToDelete!!.amount
                val newBalance = (currentBalance - deletedEffect).coerceAtLeast(0.0)
                viewModel.deleteSavingsTransaction(txToDelete!!)
                syncGoalBalance(newBalance, currentBalance)
                txToDelete = null
            },
            onDismiss = { txToDelete = null }
        )
    }
}

@Composable
fun GoalStatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────
//  TRANSACTION CARD
// ─────────────────────────────────────────────────
@Composable
fun SavingsTransactionCard(
    tx: SavingsTransactionEntity,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = if (tx.isDeposit) Color(0xFF30D158) else ExpenseRed
    val label = if (tx.isDeposit) "জমা" else "উত্তোলন"
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        glassAlpha = if (isDark) 0.10f else 0.06f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (tx.isDeposit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    null, tint = accentColor, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                    if (tx.date.isNotBlank()) {
                        Text(tx.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (tx.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(tx.note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("${if (tx.isDeposit) "+" else "-"}৳${tx.amount.toBanglaString()}",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(modifier = Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("এডিট", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("ডিলিট", color = ExpenseRed, fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = ExpenseRed) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  ADD / EDIT GOAL DIALOG
// ─────────────────────────────────────────────────
@Composable
fun AddSavingsGoalDialog(
    isDark: Boolean,
    existingGoal: SavingsGoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, targetDate: String) -> Unit
) {
    val isEdit = existingGoal != null
    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    // Fix: use raw integer/decimal string — toBanglaString() adds commas which break toEnglishDouble()
    var targetText by remember {
        mutableStateOf(
            existingGoal?.targetAmount?.let { amt ->
                if (amt > 0) {
                    // Store as plain digits without comma separators
                    if (amt == amt.toLong().toDouble()) amt.toLong().toString() else amt.toString()
                } else ""
            } ?: ""
        )
    }
    var selectedDate by remember { mutableStateOf(existingGoal?.targetDate ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fieldBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val fieldContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = cardBg,
            glassAlpha = if (isDark) 0.12f else 0.04f
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (isEdit) "সঞ্চয় এডিট" else "নতুন সঞ্চয়",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("সঞ্চয়ের নাম", color = hintColor) },
                    placeholder = { Text("যেমন: নতুন বাইক, হজ তহবিল", color = hintColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedBorderColor = Color(0xFF30D158), unfocusedBorderColor = fieldBorderColor,
                        focusedContainerColor = fieldContainerColor, unfocusedContainerColor = fieldContainerColor
                    ),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetText, onValueChange = { targetText = it.toEnglishDigits() },
                    label = { Text("লক্ষ্যমাত্রা (৳)", color = hintColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedBorderColor = Color(0xFF30D158), unfocusedBorderColor = fieldBorderColor,
                        focusedContainerColor = fieldContainerColor, unfocusedContainerColor = fieldContainerColor
                    ),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(fieldContainerColor)
                        .clickable(remember { MutableInteractionSource() }, null) { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("লক্ষ্যের তারিখ", fontSize = 12.sp, color = hintColor)
                            Text(
                                if (selectedDate.isBlank()) "তারিখ নির্বাচন করুন" else selectedDate,
                                fontSize = 15.sp,
                                color = if (selectedDate.isBlank()) hintColor else textColor,
                                fontWeight = if (selectedDate.isBlank()) FontWeight.Normal else FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF30D158), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("বাতিল", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && targetText.isNotBlank()) {
                                onSave(title, targetText.toEnglishDouble(), selectedDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(if (isEdit) "আপডেট" else "তৈরি করুন", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                    selectedDate = "${cal.get(java.util.Calendar.DAY_OF_MONTH).toBanglaString()} ${banglaMonths[cal.get(java.util.Calendar.MONTH)]}, ${cal.get(java.util.Calendar.YEAR).toBanglaString()}"
                }
                showDatePicker = false
            }
        )
    }
}

// ─────────────────────────────────────────────────
//  SAVINGS ENTRY DIALOG
// ─────────────────────────────────────────────────
@Composable
fun SavingsEntryDialog(
    isDark: Boolean,
    isDeposit: Boolean,
    existingTx: SavingsTransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String, date: String, isDeposit: Boolean) -> Unit
) {
    // Fix: use plain number string (no commas) so toEnglishDouble() works correctly
    var amountText by remember {
        mutableStateOf(
            existingTx?.amount?.let { amt ->
                if (amt > 0) {
                    if (amt == amt.toLong().toDouble()) amt.toLong().toString() else amt.toString()
                } else ""
            } ?: ""
        )
    }
    var note by remember { mutableStateOf(existingTx?.note ?: "") }
    var selectedDate by remember { mutableStateOf(existingTx?.date ?: "") }
    var currentIsDeposit by remember { mutableStateOf(existingTx?.isDeposit ?: isDeposit) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEdit = existingTx != null
    val accentColor = if (currentIsDeposit) Color(0xFF30D158) else ExpenseRed
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fieldBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val fieldContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = cardBg,
            glassAlpha = if (isDark) 0.12f else 0.04f
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (isEdit) "লেনদেন সম্পাদনা" else if (currentIsDeposit) "জমা দিন" else "উত্তোলন",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Type toggle (edit mode only)
                if (isEdit) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (currentIsDeposit) Color(0xFF30D158) else Color.Transparent)
                                .clickable(remember { MutableInteractionSource() }, null) { currentIsDeposit = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("জমা", fontSize = 14.sp,
                                fontWeight = if (currentIsDeposit) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentIsDeposit) Color.White else textColor)
                        }
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (!currentIsDeposit) ExpenseRed else Color.Transparent)
                                .clickable(remember { MutableInteractionSource() }, null) { currentIsDeposit = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("উত্তোলন", fontSize = 14.sp,
                                fontWeight = if (!currentIsDeposit) FontWeight.Bold else FontWeight.Normal,
                                color = if (!currentIsDeposit) Color.White else textColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it.toEnglishDigits() },
                    label = { Text("পরিমাণ (৳)", color = hintColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor, unfocusedBorderColor = fieldBorderColor,
                        focusedContainerColor = fieldContainerColor, unfocusedContainerColor = fieldContainerColor
                    ),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("নোট (ঐচ্ছিক)", color = hintColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor, unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor, unfocusedBorderColor = fieldBorderColor,
                        focusedContainerColor = fieldContainerColor, unfocusedContainerColor = fieldContainerColor
                    ),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(fieldContainerColor)
                        .clickable(remember { MutableInteractionSource() }, null) { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("তারিখ", fontSize = 12.sp, color = hintColor)
                            Text(
                                if (selectedDate.isBlank()) "তারিখ নির্বাচন করুন" else selectedDate,
                                fontSize = 15.sp,
                                color = if (selectedDate.isBlank()) hintColor else textColor,
                                fontWeight = if (selectedDate.isBlank()) FontWeight.Normal else FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Default.CalendarToday, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("বাতিল", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val amount = amountText.toEnglishDouble()
                            if (amount > 0) onSave(amount, note, selectedDate, currentIsDeposit)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("সংরক্ষণ", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                    selectedDate = "${cal.get(java.util.Calendar.DAY_OF_MONTH).toBanglaString()} ${banglaMonths[cal.get(java.util.Calendar.MONTH)]}, ${cal.get(java.util.Calendar.YEAR).toBanglaString()}"
                }
                showDatePicker = false
            }
        )
    }
}

// ─────────────────────────────────────────────────
//  DELETE CONFIRM DIALOG
// ─────────────────────────────────────────────────
@Composable
fun SavingsDeleteConfirmDialog(
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = cardBg,
            glassAlpha = if (isDark) 0.12f else 0.04f
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(ExpenseRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = ExpenseRed, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("লেনদেন মুছবেন?", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Text("এই লেনদেনটি মুছে ফেলা হবে।", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                    ) {
                        Text("মুছুন", color = Color.White)
                    }
                }
            }
        }
    }
}

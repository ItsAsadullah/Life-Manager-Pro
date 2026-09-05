package com.hisabnikash.app.ui.dashboard


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.data.local.TransactionEntity
import com.hisabnikash.app.data.local.WalletEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    categories: List<String>,
    wallets: List<WalletEntity> = emptyList(),
    onManageCategories: () -> Unit,
    onManageWallets: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onUpdate: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableIntStateOf(if (transaction.isIncome) 0 else 1) } 
    var amount by remember { mutableStateOf(if (transaction.amount == transaction.amount.toLong().toDouble()) transaction.amount.toLong().toString() else transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.title) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var dateText by remember { mutableStateOf(transaction.date) }
    var timeText by remember { mutableStateOf(transaction.time) }
    var isRecurring by remember { mutableStateOf(transaction.isRecurring) }
    var recurringPeriod by remember { mutableStateOf(transaction.recurringPeriod ?: "Daily") }

    val defaultWallet = wallets.find { it.isDefault }?.name ?: wallets.firstOrNull()?.name ?: "নগদ ক্যাশ"
    var selectedWalletName by remember(transaction, wallets) { 
        val method = transaction.paymentMethod
        val resolved = if (method.isNullOrBlank() || method == "ক্যাশ") defaultWallet else method
        mutableStateOf(resolved)
    }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))

    var showRecurringSheet by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).systemBarsPadding().imePadding().pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(32.dp), backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), glassAlpha = 0.1f) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.clickable { onDismiss() })
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "আপডেট করুন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            }
                            IconBox(icon = Icons.Default.Timer, tint = if (isRecurring) Color(0xFF0A84FF) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), onClick = { if (isRecurring) isRecurring = false else showRecurringSheet = true })
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateOrTimeButton(modifier = Modifier.weight(1f), icon = Icons.Default.DateRange, text = dateText, onClick = { showDatePicker = true })
                            DateOrTimeButton(modifier = Modifier.weight(1f), icon = Icons.Default.Schedule, text = timeText, onClick = { showTimePicker = true })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TransactionTypeToggle(selectedIndex = selectedType, onItemSelected = { selectedType = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        AccountSelectorRow(
                            isIncome = selectedType == 0,
                            wallets = wallets,
                            selectedWalletName = selectedWalletName,
                            onWalletSelected = { selectedWalletName = it },
                            onManageWallets = onManageWallets
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CategorySelectorRow(
                            categories = categories, 
                            selectedCategory = selectedCategory, 
                            onCategorySelected = { selectedCategory = it },
                            onManageCategories = onManageCategories
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        GlassTextField(
                            value = amount, 
                            onValueChange = { amount = it.toEnglishDigits() }, 
                            label = "টাকার পরিমাণ", 
                            keyboardType = KeyboardType.Number,
                            readOnly = true,
                            onFocusChanged = { focused -> showCalculator = focused }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassTextField(value = note, onValueChange = { note = it }, label = "বিবরণ (অপশনাল)", keyboardType = KeyboardType.Text, onFocusChanged = { if (it) showCalculator = false })
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val parsedAmount = amount.toEnglishDouble()
                                if (parsedAmount > 0) {
                                    onUpdate(
                                        transaction.copy(
                                            title = note.ifBlank { selectedCategory.ifBlank { if (selectedType == 0) "আয়" else "ব্যয়" } },
                                            amount = parsedAmount,
                                            isIncome = selectedType == 0,
                                            date = dateText,
                                            time = timeText,
                                            category = selectedCategory,
                                            isRecurring = isRecurring,
                                            recurringPeriod = recurringPeriod,
                                            paymentMethod = selectedWalletName
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("আপডেট করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }
                        
                        if (showCalculator) {
                            Spacer(modifier = Modifier.height(24.dp))
                            CustomCalculatorKeyboard(
                                value = amount,
                                onValueChange = { amount = it.toEnglishDigits() }
                            )
                        }
                    }
                }
            }
        }
        
        if (showRecurringSheet) {
            RecurringSetupBottomSheet(
                onDismissRequest = { showRecurringSheet = false },
                initialPeriod = recurringPeriod,
                calendar = calendar,
                onDateOrTimeChange = {
                    dateText = dateFormat.format(calendar.time)
                    timeText = timeFormat.format(calendar.time)
                },
                onSave = { selectedPeriod ->
                    recurringPeriod = selectedPeriod
                    isRecurring = true
                    showRecurringSheet = false
                }
            )
        }
        if (showDatePicker) {
            IOSDatePickerDialog(
                onDismiss = { showDatePicker = false },
                onDateSelected = { time -> 
                    if (time != null) {
                        calendar.timeInMillis = time
                        dateText = dateFormat.format(calendar.time)
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
                    timeText = timeFormat.format(calendar.time)
                    showTimePicker = false
                }
            )
        }
    }
}

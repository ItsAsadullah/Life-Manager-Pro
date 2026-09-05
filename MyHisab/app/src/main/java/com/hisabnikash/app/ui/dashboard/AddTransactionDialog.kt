package com.hisabnikash.app.ui.dashboard


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.data.local.WalletEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddTransactionDialog(
    categories: List<String>,
    wallets: List<WalletEntity> = emptyList(),
    onManageCategories: () -> Unit,
    onManageWallets: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (isIncome: Boolean, amount: String, note: String, date: String, time: String, category: String, isRecurring: Boolean, recurringPeriod: String?, paymentMethod: String) -> Unit,
    onSaveQuickEntry: ((isIncome: Boolean, amount: String, note: String, category: String) -> Unit)? = null,
    initialQuickEntry: com.hisabnikash.app.data.local.QuickEntryEntity? = null
) {
    val context = LocalContext.current
    var selectedType by remember { mutableIntStateOf(if (initialQuickEntry?.isIncome == false) 1 else 0) } 
    var amount by remember { mutableStateOf(initialQuickEntry?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var note by remember { mutableStateOf(initialQuickEntry?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialQuickEntry?.category ?: "") }
    
    val defaultWallet = wallets.find { it.isDefault }?.name ?: wallets.firstOrNull()?.name ?: "নগদ ক্যাশ"
    var selectedWalletName by remember(wallets) { mutableStateOf(defaultWallet) }
    
    var isQuickEntry by remember { mutableStateOf(initialQuickEntry != null) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurringPeriod by remember { mutableStateOf<String?>("Daily") }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))

    var showRecurringSheet by remember { mutableStateOf(false) }

    var dateText by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var timeText by remember { mutableStateOf(timeFormat.format(calendar.time)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).windowInsetsPadding(WindowInsets.safeDrawing).pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), shape = RoundedCornerShape(32.dp), backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), glassAlpha = 0.1f) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState())) {
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isQuickEntry) "কুইক এন্ট্রি" else "নতুন এন্ট্রি", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (initialQuickEntry == null) {
                                    IconBox(icon = Icons.Default.Bolt, tint = if (isQuickEntry) Color(0xFF0A84FF) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), onClick = { isQuickEntry = !isQuickEntry })
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                IconBox(icon = Icons.Default.Timer, tint = if (isRecurring) Color(0xFF0A84FF) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), onClick = { if (isRecurring) isRecurring = false else showRecurringSheet = true })
                                Spacer(modifier = Modifier.width(8.dp))
                                IconBox(icon = Icons.Default.Close, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), onClick = onDismiss)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isQuickEntry) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DateOrTimeButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.DateRange,
                                    text = dateText,
                                    onClick = { showDatePicker = true }
                                )
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
                                DateOrTimeButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Schedule,
                                    text = timeText,
                                    onClick = { showTimePicker = true }
                                )
                                if (showTimePicker) {
                                    IOSTimePickerDialog(
                                        onDismiss = { showTimePicker = false },
                                        onTimeSelected = { hourOfDay, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            calendar.set(Calendar.MINUTE, minute)
                                            timeText = timeFormat.format(calendar.time)
                                            showTimePicker = false
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        TransactionTypeToggle(selectedIndex = selectedType, onItemSelected = { selectedType = it })

                        Spacer(modifier = Modifier.height(12.dp))

                        AccountSelectorRow(
                            isIncome = selectedType == 0,
                            wallets = wallets,
                            selectedWalletName = selectedWalletName,
                            onWalletSelected = { selectedWalletName = it },
                            onManageWallets = onManageWallets
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CategorySelectorRow(
                            categories = categories, 
                            selectedCategory = selectedCategory, 
                            onCategorySelected = { selectedCategory = it },
                            onManageCategories = onManageCategories
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        GlassTextField(
                            value = amount, 
                            onValueChange = { amount = it.toEnglishDigits() }, 
                            label = "টাকার পরিমাণ", 
                            keyboardType = KeyboardType.Number,
                            readOnly = true,
                            onFocusChanged = { focused -> showCalculator = focused }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassTextField(value = note, onValueChange = { note = it }, label = "বিবরণ (অপশনাল)", keyboardType = KeyboardType.Text, onFocusChanged = { if (it) showCalculator = false })
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (amount.isNotEmpty()) {
                                    if (isQuickEntry && onSaveQuickEntry != null) {
                                        onSaveQuickEntry(selectedType == 0, amount.toEnglishDigits(), note, selectedCategory)
                                    } else {
                                        onSave(selectedType == 0, amount.toEnglishDigits(), note, dateText, timeText, selectedCategory, isRecurring, recurringPeriod, selectedWalletName)
                                    }
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(if (initialQuickEntry != null) "আপডেট করুন" else "অ্যাড করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }
                        
                        if (showCalculator) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CustomCalculatorKeyboard(
                                value = amount,
                                onValueChange = { amount = it.toEnglishDigits() }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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

        // Using standard Android Date and Time pickers directly in onClick
    }
}

@Composable
fun CategorySelectorRow(
    categories: List<String>, 
    selectedCategory: String, 
    onCategorySelected: (String) -> Unit,
    onManageCategories: () -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCats = categories.filter { it.contains(searchQuery, ignoreCase = true) }

    Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isSearchExpanded) {
            Box(modifier = Modifier.width(140.dp).fillMaxHeight().border(1.dp, BalanceBlue, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, textStyle = TextStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 13.sp), singleLine = true, modifier = Modifier.weight(1f), decorationBox = { inner -> if (searchQuery.isEmpty()) Text("সার্চ...", color = TextWhiteSecondary, fontSize = 13.sp); inner() })
                    Icon(Icons.Default.Close, null, tint = TextWhiteSecondary, modifier = Modifier.size(16.dp).clickable { isSearchExpanded = false; searchQuery = "" })
                }
            }
        } else {
            Box(modifier = Modifier.size(40.dp).border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape).clickable { isSearchExpanded = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, null, tint = TextWhiteSecondary, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(filteredCats) { cat ->
                val isSelected = selectedCategory == cat
                Box(modifier = Modifier.height(28.dp).clip(RoundedCornerShape(20.dp)).background(if (isSelected) BalanceBlue else Color.Transparent).border(1.dp, if (isSelected) Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).clickable { onCategorySelected(if (isSelected) "" else cat) }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(cat, color = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                }
            }
            item {
                Box(modifier = Modifier.size(28.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape).clickable { onManageCategories() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ... [IconBox, DateOrTimeButton, TransactionTypeToggle, GlassTextField অপরিবর্তিত থাকবে]
@Composable
fun IconBox(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DateOrTimeButton(modifier: Modifier, icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(modifier = modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).clickable { onClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TransactionTypeToggle(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).padding(4.dp)) {
        val isIncome = selectedIndex == 0
        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp)).background(if (isIncome) Color(0xFF34C759) else Color.Transparent).clickable { onItemSelected(0) }, contentAlignment = Alignment.Center) {
            Text("আয়", fontSize = 16.sp, fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium, color = if (isIncome) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        val isExpense = selectedIndex == 1
        Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp)).background(if (isExpense) Color(0xFFFF3B30) else Color.Transparent).clickable { onItemSelected(1) }, contentAlignment = Alignment.Center) {
            Text("ব্যয়", fontSize = 16.sp, fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium, color = if (isExpense) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        readOnly = readOnly,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onFocusChanged != null) Modifier.onFocusChanged { onFocusChanged(it.isFocused) } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            cursorColor = Color(0xFF0A84FF)
        )
    )
}

@Composable
fun AccountSelectorRow(
    isIncome: Boolean,
    wallets: List<WalletEntity>,
    selectedWalletName: String,
    onWalletSelected: (String) -> Unit,
    onManageWallets: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isIncome) "যে অ্যাকাউন্টে টাকা ঢুকছে (জমা):" else "যে অ্যাকাউন্ট থেকে খরচ হচ্ছে:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isIncome) Color(0xFF34C759) else Color(0xFFFF453A)
            )
            if (onManageWallets != null) {
                Text(
                    text = "ম্যানেজ",
                    fontSize = 11.sp,
                    color = Color(0xFF0A84FF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onManageWallets() }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayWallets = if (wallets.isEmpty()) {
                listOf(WalletEntity(name = "নগদ ক্যাশ", accountType = "CASH", balance = 0.0, isDefault = true))
            } else {
                wallets
            }
            items(displayWallets) { wallet ->
                val isSelected = selectedWalletName == wallet.name
                val color = Color(wallet.colorHex)
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) color.copy(alpha = 0.18f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) color else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onWalletSelected(wallet.name) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (wallet.accountType) {
                                "CASH" -> Icons.Default.Wallet
                                "BKASH", "NAGAD", "ROCKET", "UPAY" -> Icons.Default.PhoneAndroid
                                "BANK" -> Icons.Default.AccountBalance
                                "CARD" -> Icons.Default.CreditCard
                                else -> Icons.Default.AccountBalanceWallet
                            },
                            contentDescription = null,
                            tint = if (isSelected) color else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = wallet.name,
                            color = if (isSelected) color else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.data.local.PersonEntity
import com.hisabnikash.app.data.local.PersonTransactionEntity
import com.hisabnikash.app.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape

@Composable
fun PersonDetailScreen(
    person: PersonEntity,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.getTransactionsForPerson(person.id).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditPersonDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<PersonTransactionEntity?>(null) }
    var showSuccessDialog by remember { mutableStateOf<SuccessDialogData?>(null) }

    val totalReceive = transactions.filter { it.isReceive }.sumOf { it.amount }
    val totalGive = transactions.filter { !it.isReceive }.sumOf { it.amount }
    val netBalance = totalReceive - totalGive

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .padding(bottom = 64.dp, end = 16.dp),
                    containerColor = Color(0xFF0A84FF).copy(alpha = 0.4f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(32.dp))
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
            ) {
                LiquidBackgroundGlow()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-8).dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            val photoUri = person.photoUri
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(42.dp).background(BalanceBlue.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BalanceBlue, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    person.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val subtitle = listOf(person.phone, person.address).filter { it.isNotBlank() }.joinToString(" • ")
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, fontSize = 12.sp, color = TextWhiteSecondary)
                                }
                            }
                        }

                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = 8.dp)) {
                            if (person.phone.isNotBlank()) {
                                IconButton(onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:${person.phone}")
                                    )
                                    context.startActivity(intent)
                                }) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = TextWhiteSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://api.whatsapp.com/send?phone=${person.phone}")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "WhatsApp not installed",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val personTransactions =
                                    viewModel.allPersonTransactions.value.filter { it.personId == person.id }
                                PdfExportUtil.generateAndSharePdf(context, person, personTransactions)
                            }) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "Download Report",
                                    tint = TextWhiteSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Net Balance Summary Card
                    val balanceColor = if (netBalance >= 0) IncomeGreen else ExpenseRed
                    val balanceTitle = if (netBalance >= 0) "পাবো" else "দিবো"
                    val balanceIcon =
                        if (netBalance >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        glassAlpha = 0.08f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    balanceIcon,
                                    contentDescription = null,
                                    tint = balanceColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    balanceTitle,
                                    fontSize = 16.sp,
                                    color = balanceColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "৳ ${kotlin.math.abs(netBalance).toBanglaString()}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = balanceColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Calculate Running Balance for Ledger
                    val sortedAsc = transactions.sortedBy { it.id }
                    var currentBal = 0.0
                    val ledgerItems = sortedAsc.map { tx ->
                        val amt = if (tx.isReceive) tx.amount else -tx.amount
                        currentBal += amt
                        tx to currentBal
                    }.reversed()

                    // Transactions Ledger
                    if (ledgerItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyStateContent(
                                icon = Icons.Default.History,
                                title = "কোন লেনদেনের ইতিহাস নেই",
                                subtitle = "নিচের + বাটনে চাপ দিয়ে লেনদেন যোগ করুন"
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFEFEF))
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "বিবরণ",
                                    modifier = Modifier.weight(1.5f).padding(start = 12.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                Text(
                                    "দিলাম",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "পেলাম",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    textAlign = TextAlign.Center
                                )
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 88.dp)
                            ) {
                                items(ledgerItems) { (tx, bal) ->
                                    val isGiven = tx.isReceive // isReceive == true means I gave it (Receivable)
                                    var menuExpanded by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // বিবরণ
                                        Column(
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                if (tx.note.isNotBlank()) tx.note else if (isGiven) "দিলাম" else "পেলাম",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "${tx.date}, ${tx.time}",
                                                fontSize = 11.sp,
                                                color = TextWhiteSecondary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val badgeColor = if (bal >= 0) IncomeGreen else ExpenseRed
                                            val badgeText = if (bal >= 0) "পাবো" else "দিবো"
                                            Box(
                                                modifier = Modifier
                                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "${kotlin.math.abs(bal).toBanglaString()} $badgeText",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor
                                                )
                                            }
                                        }

                                        // দিলাম (Given) Column
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(ExpenseRed.copy(alpha = 0.05f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isGiven) {
                                                Text(
                                                    tx.amount.toBanglaString(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = ExpenseRed,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        // পেলাম (Received) Column
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(IncomeGreen.copy(alpha = 0.05f))
                                                .padding(12.dp)
                                                .clickable(onClick = { menuExpanded = true }),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!isGiven) {
                                                Text(
                                                    tx.amount.toBanglaString(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = IncomeGreen,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            // Option icon at the top right of this cell
                                            Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)) {
                                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextWhiteSecondary, modifier = Modifier.size(16.dp))
                                                }
                                                DropdownMenu(
                                                    expanded = menuExpanded,
                                                    onDismissRequest = { menuExpanded = false },
                                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("এডিট", color = MaterialTheme.colorScheme.onSurface) },
                                                        onClick = { menuExpanded = false; transactionToEdit = tx }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("ডিলিট", color = ExpenseRed) },
                                                        onClick = { menuExpanded = false; viewModel.deletePersonTransaction(tx) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                                }

                                // Total Row Footer
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFEFEFEF))
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "মোট",
                                            modifier = Modifier.weight(1.5f).padding(start = 12.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                totalReceive.toBanglaString(),
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRed
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                totalGive.toBanglaString(),
                                                fontWeight = FontWeight.Bold,
                                                color = IncomeGreen
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

    if (showAddDialog) {
        AddPersonTransactionDialog(
            personName = person.name,
            onDismiss = { showAddDialog = false },
            onSave = { isReceive, amountStr, note, date, time ->
                val amount = amountStr.toEnglishDouble()
                if (amount > 0) {
                    val newTx = PersonTransactionEntity(
                        personId = person.id,
                        amount = amount,
                        isReceive = isReceive,
                        note = note,
                        date = date,
                        time = time
                    )
                    viewModel.insertPersonTransaction(newTx)
                    
                    val pBal = netBalance
                    val amt = if (isReceive) amount else -amount
                    val cBal = pBal + amt
                    showSuccessDialog = SuccessDialogData(
                        personName = person.name,
                        personPhone = person.phone,
                        note = note.ifBlank { if (isReceive) "দিয়েছি (পাবো)" else "নিয়েছি (দিবো)" },
                        date = date,
                        isReceive = isReceive,
                        previousBalance = pBal,
                        transactionAmount = amount,
                        currentBalance = cBal
                    )
                }
                showAddDialog = false
            }
        )
    }

    showSuccessDialog?.let { data ->
        SuccessTransactionDialog(
            data = data,
            onDismiss = { showSuccessDialog = null }
        )
    }

    if (transactionToEdit != null) {
        AddPersonTransactionDialog(
            personName = person.name,
            initialTransaction = transactionToEdit,
            onDismiss = { transactionToEdit = null },
            onSave = { isReceive, amountStr, note, date, time ->
                val amount = amountStr.toEnglishDouble()
                if (amount > 0) {
                    val updated = transactionToEdit!!.copy(
                        amount = amount,
                        isReceive = isReceive,
                        note = note,
                        date = date,
                        time = time
                    )
                    viewModel.updatePersonTransaction(updated)
                }
                transactionToEdit = null
            }
        )
    }

    if (showEditPersonDialog) {
        AddPersonScreen(
            initialPerson = person,
            onDismiss = { showEditPersonDialog = false },
            onSave = { name, phone, address, date, photoUri ->
                val updatedPerson = person.copy(
                    name = name,
                    phone = phone,
                    address = address,
                    photoUri = photoUri ?: person.photoUri
                )
                viewModel.updatePerson(updatedPerson)
                showEditPersonDialog = false
            }
        )
    }
}

@Composable
fun AddPersonTransactionDialog(
    personName: String,
    initialTransaction: PersonTransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (isReceive: Boolean, amount: String, note: String, date: String, time: String) -> Unit
) {
    var isReceive by remember { mutableStateOf(initialTransaction?.isReceive ?: true) }
    var amount by remember {
        mutableStateOf(
            initialTransaction?.amount?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "US"))

    var dateText by remember { mutableStateOf(initialTransaction?.date ?: dateFormat.format(calendar.time)) }
    var timeText by remember { mutableStateOf(initialTransaction?.time ?: timeFormat.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                glassAlpha = 0.1f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (initialTransaction == null) "$personName-এর লেনদেন" else "লেনদেন এডিট",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Type Segmented Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isReceive) IncomeGreen else Color.Transparent)
                                .clickable { isReceive = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "দিয়েছি (পাবো)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReceive) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isReceive) ExpenseRed else Color.Transparent)
                                .clickable { isReceive = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "নিয়েছি (দিবো)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isReceive) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date & Time buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = amount,
                        onValueChange = { amount = it.toEnglishDigits() },
                        label = "টাকার পরিমাণ",
                        keyboardType = KeyboardType.Number,
                        readOnly = true,
                        onFocusChanged = { focused -> showCalculator = focused }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "বিবরণ / নোট (ঐচ্ছিক)",
                        keyboardType = KeyboardType.Text,
                        onFocusChanged = { if (it) showCalculator = false }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (showCalculator) {
                        CustomCalculatorKeyboard(
                            value = amount,
                            onValueChange = { amount = it.toEnglishDigits() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            if (amount.isNotBlank()) {
                                onSave(isReceive, amount, note, dateText, timeText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReceive) IncomeGreen else ExpenseRed
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (initialTransaction == null) "সংরক্ষণ করুন" else "আপডেট করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

data class SuccessDialogData(
    val personName: String,
    val personPhone: String?,
    val note: String,
    val date: String,
    val isReceive: Boolean,
    val previousBalance: Double,
    val transactionAmount: Double,
    val currentBalance: Double
)

@Composable
fun SuccessTransactionDialog(
    data: SuccessDialogData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF007AFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("লেনদেনটি অ্যাড হয়েছে।", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Details Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        // Person Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.LightGray.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(data.personName.take(2), fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(data.personName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                if (data.personPhone != null) {
                                    Text(data.personPhone, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            val badgeColor = if (data.isReceive) ExpenseRed else IncomeGreen
                            val badgeText = if (data.isReceive) "দিলাম" else "পেলাম"
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(badgeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Note & Date
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("বিবরণ", color = Color.Gray, fontSize = 14.sp)
                            }
                            Text(data.note, color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("তারিখ", color = Color.Gray, fontSize = 14.sp)
                            }
                            Text(data.date, color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Balances
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("পূর্বের " + if (data.previousBalance >= 0) "পাবো" else "দিবো", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                kotlin.math.abs(data.previousBalance).toBanglaString(), 
                                color = if (data.previousBalance >= 0) IncomeGreen else ExpenseRed, 
                                fontWeight = FontWeight.Medium, 
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text((if (data.isReceive) "↑ " else "↓ ") + if (data.isReceive) "দিলাম" else "পেলাম", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                data.transactionAmount.toBanglaString(), 
                                color = if (data.isReceive) ExpenseRed else IncomeGreen, 
                                fontWeight = FontWeight.Medium, 
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("বর্তমান " + if (data.currentBalance >= 0) "পাবো" else "দিবো", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                kotlin.math.abs(data.currentBalance).toBanglaString(), 
                                color = if (data.currentBalance >= 0) IncomeGreen else ExpenseRed, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF007AFF))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ঠিক আছে", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF007AFF))
                            .clickable { 
                                val shareText = """
                                    লেনদেন রেকর্ড
                                    কাস্টমার: ${data.personName}
                                    কাস্টমার মোবাইল: ${data.personPhone ?: ""}
                                    ${data.date}
                                    পূর্বে ${if (data.previousBalance >= 0) "পাবো" else "দিবো"}: ${kotlin.math.abs(data.previousBalance).toBanglaString()}
                                    ${if (data.isReceive) "দিলাম" else "পেলাম"}: ${data.transactionAmount.toBanglaString()}
                                    বর্তমান ${if (data.currentBalance >= 0) "পাবো" else "দিবো"}: ${kotlin.math.abs(data.currentBalance).toBanglaString()}
                                    বিবরণ: ${data.note}
                                """.trimIndent()
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "শেয়ার করুন"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            }
        }
    }
}
package com.hisabnikash.app.ui.wallet

import android.widget.Toast
import com.hisabnikash.app.data.local.TransactionEntity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hisabnikash.app.data.local.WalletEntity
import com.hisabnikash.app.ui.dashboard.CustomCalculatorKeyboard
import com.hisabnikash.app.ui.dashboard.TransactionViewModel
import com.hisabnikash.app.ui.dashboard.toEnglishDigits
import com.hisabnikash.app.ui.dashboard.toEnglishDouble
import com.hisabnikash.app.ui.market.IOSInputField
import java.util.Locale

// ওয়ালেট কালার প্যালেট
private val WalletColors = listOf(
    0xFF34C759, // Green / Cash
    0xFFE2136E, // bKash Pink
    0xFFF7941D, // Nagad Orange
    0xFF0A84FF, // Bank Blue
    0xFF8C3494, // Rocket Purple
    0xFF5856D6, // Indigo
    0xFFFF2D55, // Rose
    0xFFFF9500, // Amber
    0xFF30B0C7  // Teal
)

private data class WalletTypeOption(
    val type: String,
    val label: String,
    val icon: ImageVector,
    val defaultColor: Long
)

private val WalletTypeOptions = listOf(
    WalletTypeOption("CASH", "নগদ ক্যাশ", Icons.Default.Wallet, 0xFF34C759),
    WalletTypeOption("BKASH", "বিকাশ", Icons.Default.PhoneAndroid, 0xFFE2136E),
    WalletTypeOption("NAGAD", "নগদ", Icons.Default.PhoneAndroid, 0xFFF7941D),
    WalletTypeOption("ROCKET", "রকেট", Icons.Default.PhoneAndroid, 0xFF8C3494),
    WalletTypeOption("BANK", "ব্যাংক", Icons.Default.AccountBalance, 0xFF0A84FF),
    WalletTypeOption("CARD", "কার্ড", Icons.Default.CreditCard, 0xFF5856D6),
    WalletTypeOption("OTHER", "অন্যান্য", Icons.Default.AccountBalanceWallet, 0xFF30B0C7)
)

private fun getWalletIcon(accountType: String): ImageVector {
    return when (accountType) {
        "CASH" -> Icons.Default.Wallet
        "BKASH", "NAGAD", "ROCKET", "UPAY" -> Icons.Default.PhoneAndroid
        "BANK" -> Icons.Default.AccountBalance
        "CARD" -> Icons.Default.CreditCard
        else -> Icons.Default.AccountBalanceWallet
    }
}

private fun getWalletTypeBangla(accountType: String): String {
    return when (accountType) {
        "CASH" -> "ক্যাশ"
        "BKASH" -> "বিকাশ"
        "NAGAD" -> "নগদ"
        "ROCKET" -> "রকেট"
        "BANK" -> "ব্যাংক একাউন্ট"
        "CARD" -> "কার্ড"
        else -> "ওয়ালেট"
    }
}

/**
 * প্রফেশনাল ওয়ালেট ও অ্যাকাউন্ট তৈরি ও ব্যবস্থাপনা স্ক্রিন
 */
@Composable
fun WalletManagementScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawWallets by viewModel.allWallets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    // প্রাথমিক অবস্থায় কোনো ওয়ালেট তৈরি না করা থাকলেও সর্বদা নগদ ক্যাশ ওয়ালেট থাকবে
    val wallets = remember(rawWallets) {
        if (rawWallets.isEmpty()) {
            listOf(WalletEntity.createDefaultCashWallet())
        } else {
            rawWallets
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // ডায়ালগ কন্ট্রোল
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var walletToDelete by remember { mutableStateOf<WalletEntity?>(null) }

    // লেনদেনটি নির্দিষ্ট অ্যাকাউন্টের সাথে মিলে কিনা তা যাচাই
    fun isTxForWallet(tx: TransactionEntity, wallet: WalletEntity): Boolean {
        if (tx.paymentMethod == wallet.name) return true
        val isCashWallet = wallet.name == "নগদ ক্যাশ" || wallet.accountType == "CASH"
        if (isCashWallet && (tx.paymentMethod == "ক্যাশ" || tx.paymentMethod == "নগদ ক্যাশ" || tx.paymentMethod.isNullOrBlank())) {
            return true
        }
        if (wallet.isDefault && tx.paymentMethod.isNullOrBlank()) {
            return true
        }
        return false
    }

    // প্রতিটি ওয়ালেটের লাইভ ব্যালেন্স হিসাব ফাংশন: প্রারম্ভিক ব্যালেন্স + আয় - ব্যয়
    fun getWalletLiveBalance(wallet: WalletEntity): Double {
        val opening = wallet.balance
        val income = transactions.filter { tx ->
            tx.isIncome && isTxForWallet(tx, wallet)
        }.sumOf { it.amount }
        val expense = transactions.filter { tx ->
            !tx.isIncome && isTxForWallet(tx, wallet)
        }.sumOf { it.amount }
        return opening + income - expense
    }

    // মোট লাইভ নেট ব্যালেন্স ও মোট প্রারম্ভিক ব্যালেন্স
    val totalBalance = remember(wallets, transactions) {
        wallets.sumOf { getWalletLiveBalance(it) }
    }
    val totalOpeningBalance = remember(wallets) {
        wallets.sumOf { it.balance }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // ১. শীর্ষ নেভিগেশন ও হেডার
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "অ্যাকাউন্ট ও ওয়ালেট",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${wallets.size} টি অ্যাকাউন্ট সক্রিয়",
                            fontSize = 12.sp,
                            color = subtextColor
                        )
                    }
                }

                // "+ নতুন ওয়ালেট" বাটন
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0A84FF))
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "নতুন ওয়ালেট",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ২. মোট নেট ব্যালেন্স কার্ড (Apple Wallet Style Hero Banner)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E3C72),
                                    Color(0xFF2A5298)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট উপলব্ধ ব্যালেন্স",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )

                            // টাকা ট্রান্সফার বাটন
                            if (wallets.size >= 2) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { showTransferDialog = true }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ট্রান্সফার",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "৳ ${String.format(Locale.getDefault(), "%,.2f", totalBalance)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val defaultWallet = wallets.find { it.isDefault }
                            if (defaultWallet != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF34C759).copy(alpha = 0.35f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ডিফল্ট: ${defaultWallet.name}",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (totalOpeningBalance > 0.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "প্রারম্ভিক: ৳ ${String.format(Locale.getDefault(), "%,.2f", totalOpeningBalance)}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ৩. অ্যাকাউন্ট ও ওয়ালেটের তালিকা (Apple Inset Grouped Table)
            Text(
                text = "আমার অ্যাকাউন্টসমূহ",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = subtextColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (wallets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = subtextColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "কোনো অ্যাকাউন্ট পাওয়া যায়নি",
                            fontSize = 15.sp,
                            color = subtextColor
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(cardBorderColor, cardBorderColor)))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(wallets, key = { it.id }) { wallet ->
                            val liveBal = getWalletLiveBalance(wallet)
                            val cashCount = wallets.count { it.name == "নগদ ক্যাশ" || it.accountType == "CASH" }
                            val canDeleteWallet = (!wallet.isDefault || wallets.count { it.isDefault } > 1) && 
                                                  (wallet.name != "নগদ ক্যাশ" || cashCount > 1) && 
                                                  wallets.size > 1
                            WalletRowItem(
                                wallet = wallet,
                                liveBalance = liveBal,
                                isDark = isDark,
                                canDelete = canDeleteWallet,
                                onEdit = { editingWallet = wallet },
                                onSetDefault = { viewModel.setDefaultWallet(wallet.id) },
                                onDelete = { walletToDelete = wallet }
                            )
                            if (wallet != wallets.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp),
                                    thickness = 0.6.dp,
                                    color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ====================================================
    // নতুন ওয়ালেট তৈরি ডায়ালগ
    // ====================================================
    if (showAddDialog) {
        WalletFormDialog(
            title = "নতুন ওয়ালেট যোগ করুন",
            initialWallet = null,
            onDismiss = { showAddDialog = false },
            onSave = { newWallet ->
                viewModel.insertWallet(newWallet)
                showAddDialog = false
                Toast.makeText(context, "${newWallet.name} অ্যাকাউন্ট যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ====================================================
    // ওয়ালেট এডিট ডায়ালগ
    // ====================================================
    editingWallet?.let { wallet ->
        WalletFormDialog(
            title = "অ্যাকাউন্ট সম্পাদনা",
            initialWallet = wallet,
            onDismiss = { editingWallet = null },
            onSave = { updated ->
                viewModel.updateWallet(updated)
                editingWallet = null
                Toast.makeText(context, "অ্যাকাউন্ট আপডেট হয়েছে", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ====================================================
    // টাকা ট্রান্সফার ডায়ালগ
    // ====================================================
    if (showTransferDialog && wallets.size >= 2) {
        WalletTransferDialog(
            wallets = wallets,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, toId, amount, note ->
                viewModel.transferBetweenWallets(fromId, toId, amount, note)
                showTransferDialog = false
                Toast.makeText(context, "৳ $amount সফলভাবে ট্রান্সফার করা হয়েছে!", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ====================================================
    // ডিলিট কনফার্মেশন ডায়ালগ
    // ====================================================
    walletToDelete?.let { wallet ->
        AlertDialog(
            onDismissRequest = { walletToDelete = null },
            title = { Text("অ্যাকাউন্ট মুছে ফেলবেন?") },
            text = { Text("আপনি কি নিশ্চিত যে '${wallet.name}' অ্যাকাউন্টটি মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        val cashCount = wallets.count { it.name == "নগদ ক্যাশ" || it.accountType == "CASH" }
                        val isSoleCash = (wallet.name == "নগদ ক্যাশ" || wallet.accountType == "CASH") && cashCount <= 1
                        if (isSoleCash || wallets.size <= 1) {
                            Toast.makeText(context, "একমাত্র নগদ ক্যাশ অ্যাকাউন্ট মুছে ফেলা যাবে না", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.deleteWallet(wallet)
                            Toast.makeText(context, "অ্যাকাউন্ট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                        walletToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

/**
 * ওয়ালেট রো আইটেম
 */
@Composable
private fun WalletRowItem(
    wallet: WalletEntity,
    liveBalance: Double,
    isDark: Boolean,
    canDelete: Boolean = false,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val walletColor = Color(wallet.colorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // কালার ব্যাজ আইকন
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(walletColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getWalletIcon(wallet.accountType),
                    contentDescription = null,
                    tint = walletColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wallet.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    if (wallet.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF34C759).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ডিফল্ট",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val descText = if (wallet.accountNumber.isNotBlank()) {
                    "${getWalletTypeBangla(wallet.accountType)} • ${wallet.accountNumber}"
                } else {
                    getWalletTypeBangla(wallet.accountType)
                }

                Text(
                    text = descText,
                    fontSize = 12.sp,
                    color = subtextColor
                )
            }
        }

        // ব্যালেন্স ও অপশনস
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳ ${String.format(Locale.getDefault(), "%,.2f", liveBalance)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (liveBalance >= 0) textColor else Color(0xFFFF3B30)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "প্রারম্ভিক: ৳ ${String.format(Locale.getDefault(), "%,.2f", wallet.balance)}",
                    fontSize = 10.sp,
                    color = subtextColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = subtextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("সম্পাদনা (Edit)") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    if (!wallet.isDefault) {
                        DropdownMenuItem(
                            text = { Text("ডিফল্ট সেট করুন") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSetDefault()
                            }
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("মুছে ফেলুন (Delete)", color = Color(0xFFFF3B30)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF3B30)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * নতুন ওয়ালেট তৈরি বা এডিট করার পূর্ণাঙ্গ ফর্ম ডায়ালগ
 */
@Composable
private fun WalletFormDialog(
    title: String,
    initialWallet: WalletEntity?,
    onDismiss: () -> Unit,
    onSave: (WalletEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialWallet?.name ?: "") }
    var accountType by remember { mutableStateOf(initialWallet?.accountType ?: "CASH") }
    var accountNumber by remember { mutableStateOf(initialWallet?.accountNumber ?: "") }
    var balanceText by remember { mutableStateOf(initialWallet?.balance?.toString()?.replace(".0", "") ?: "0") }
    var selectedColor by remember { mutableStateOf(initialWallet?.colorHex ?: 0xFF34C759) }
    var isDefault by remember { mutableStateOf(initialWallet?.isDefault ?: false) }
    var notes by remember { mutableStateOf(initialWallet?.notes ?: "") }
    var showCalculator by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF242426) else Color(0xFFFFFFFF)
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ১. ওয়ালেটের নাম
                IOSInputField(
                    title = "অ্যাকাউন্টের নাম",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "যেমন: নগদ ক্যাশ, বিকাশ পার্সোনাল, সিটি ব্যাংক",
                    onClick = { showCalculator = false }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ২. ওয়ালেটের ধরন (চিপস নির্বাচন)
                Text(
                    text = "অ্যাকাউন্টের ধরন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WalletTypeOptions) { option ->
                        val isSelected = accountType == option.type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(option.defaultColor).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(option.defaultColor) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    accountType = option.type
                                    if (initialWallet == null) {
                                        selectedColor = option.defaultColor
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(option.defaultColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = option.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(option.defaultColor) else textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ৩. প্রারম্ভিক ব্যালেন্স (ক্যালকুলেটর কীবোর্ড ইন্টিগ্রেটেড)
                IOSInputField(
                    title = "প্রারম্ভিক ব্যালেন্স (Opening Balance)",
                    value = if (balanceText.isNotBlank()) "৳ $balanceText" else "",
                    onValueChange = { balanceText = it.toEnglishDigits() },
                    placeholder = "টাকা নির্ধারণ করতে ট্যাপ করুন",
                    readOnly = true,
                    isFocused = showCalculator,
                    onClick = { showCalculator = !showCalculator },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showCalculator) Color(0xFF0A84FF).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { showCalculator = !showCalculator }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showCalculator) "সম্পন্ন ✓" else "🔢 ক্যালকুলেটর",
                                fontSize = 11.sp,
                                color = Color(0xFF0A84FF),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )

                if (showCalculator) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔢 ক্যালকুলেটরে ব্যালেন্স লিখুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )
                        Text(
                            text = "সম্পন্ন ✕",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subtextColor,
                            modifier = Modifier.clickable { showCalculator = false }.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomCalculatorKeyboard(
                        value = balanceText,
                        onValueChange = { balanceText = it.toEnglishDigits() }
                    )
                }

                Text(
                    text = "💡 অ্যাকাউন্ট খোলার সময় বা শুরুর ব্যালেন্স। আয় ও ব্যয়ের লেনদেনের সাথে বর্তমান ব্যালেন্স স্বয়ংক্রিয়ভাবে হিসাব হবে।",
                    fontSize = 11.sp,
                    color = subtextColor,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ৪. অ্যাকাউন্ট নম্বর
                IOSInputField(
                    title = "অ্যাকাউন্ট / মোবাইল নম্বর (ঐচ্ছিক)",
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    placeholder = "যেমন: 017XXXXXXXX",
                    onClick = { showCalculator = false }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ৫. কালার প্যালেট
                Text(
                    text = "কালার থিম",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WalletColors.forEach { colLong ->
                        val isSelected = selectedColor == colLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colLong))
                                .clickable { selectedColor = colLong },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ৬. ডিফল্ট ওয়ালেট টগল
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ডিফল্ট অ্যাকাউন্ট হিসেবে সেট করুন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                        Text(
                            text = "নতুন খরচে স্বয়ংক্রিয়ভাবে সিলেক্ট হবে",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // বাটনসমূহ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val bal = balanceText.toEnglishDouble()
                            val entity = initialWallet?.copy(
                                name = name.trim(),
                                accountType = accountType,
                                accountNumber = accountNumber.trim(),
                                balance = bal,
                                colorHex = selectedColor,
                                isDefault = isDefault,
                                notes = notes.trim()
                            ) ?: WalletEntity(
                                name = name.trim(),
                                accountType = accountType,
                                accountNumber = accountNumber.trim(),
                                balance = bal,
                                colorHex = selectedColor,
                                isDefault = isDefault,
                                notes = notes.trim()
                            )
                            onSave(entity)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = name.isNotBlank()
                    ) {
                        Text("সংরক্ষণ করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * অ্যাকাউন্ট থেকে অ্যাকাউন্টে টাকা স্থানান্তরের ডায়ালগ
 */
@Composable
private fun WalletTransferDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onTransfer: (fromId: String, toId: String, amount: Double, note: String) -> Unit
) {
    var fromWalletId by remember { mutableStateOf(wallets.first().id) }
    var toWalletId by remember {
        mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.first().id)
    }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showCalculator by remember { mutableStateOf(false) }

    val fromWallet = wallets.find { it.id == fromWalletId }
    val toWallet = wallets.find { it.id == toWalletId }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF242426) else Color(0xFFFFFFFF)
    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "টাকা স্থানান্তর (Account Transfer)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // From Wallet
                Text("উৎস অ্যাকাউন্ট (From):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(wallets) { w ->
                        val isSelected = w.id == fromWalletId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(w.colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) Color(w.colorHex) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { fromWalletId = w.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${w.name} (৳${w.balance.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // To Wallet
                Text("গন্তব্য অ্যাকাউন্ট (To):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(wallets.filter { it.id != fromWalletId }) { w ->
                        val isSelected = w.id == toWalletId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(w.colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .border(if (isSelected) 1.5.dp else 0.dp, if (isSelected) Color(w.colorHex) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { toWalletId = w.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${w.name} (৳${w.balance.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // স্থানান্তরের পরিমাণ (ক্যালকুলেটর কীবোর্ড ইন্টিগ্রেটেড)
                IOSInputField(
                    title = "স্থানান্তরের পরিমাণ (৳)",
                    value = if (amountText.isNotBlank()) "৳ $amountText" else "",
                    onValueChange = { amountText = it.toEnglishDigits() },
                    placeholder = "টাকা নির্ধারণ করতে ট্যাপ করুন",
                    readOnly = true,
                    isFocused = showCalculator,
                    onClick = { showCalculator = !showCalculator },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showCalculator) Color(0xFF0A84FF).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { showCalculator = !showCalculator }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showCalculator) "সম্পন্ন ✓" else "🔢 ক্যালকুলেটর",
                                fontSize = 11.sp,
                                color = Color(0xFF0A84FF),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )

                if (showCalculator) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔢 স্থানান্তরের পরিমাণ লিখুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )
                        Text(
                            text = "সম্পন্ন ✕",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subtextColor,
                            modifier = Modifier.clickable { showCalculator = false }.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomCalculatorKeyboard(
                        value = amountText,
                        onValueChange = { amountText = it.toEnglishDigits() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // নোট
                IOSInputField(
                    title = "নোট / বিবরণ (ঐচ্ছিক)",
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "যেমন: ক্যাশ ইন, এটিএম উত্তোলন",
                    onClick = { showCalculator = false }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toEnglishDouble()
                            if (amt > 0 && fromWalletId != toWalletId) {
                                onTransfer(fromWalletId, toWalletId, amt, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = amountText.toEnglishDouble() > 0 && fromWalletId != toWalletId
                    ) {
                        Text("ট্রান্সফার কনফার্ম করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

package com.hisabnikash.app.ui.market

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.data.local.MarketItemEntity
import com.hisabnikash.app.data.local.MarketListEntity
import com.hisabnikash.app.ui.components.GlassCard
import com.hisabnikash.app.ui.dashboard.CustomCalculatorKeyboard
import com.hisabnikash.app.ui.dashboard.EmptyStateContent
import com.hisabnikash.app.ui.dashboard.IOSDatePickerDialog
import com.hisabnikash.app.ui.dashboard.TransactionViewModel
import com.hisabnikash.app.ui.dashboard.banglaMonths
import com.hisabnikash.app.ui.dashboard.toBanglaString
import com.hisabnikash.app.ui.dashboard.toEnglishDigits
import com.hisabnikash.app.ui.dashboard.toEnglishDouble
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val MarketUnits = listOf(
    "কেজি", "গ্রাম", "লিটার", "মি.লি.", "পিস/টি", "ডজন", "প্যাকেট", "হালি", "মুঠো"
)

val PaymentMethods = listOf(
    "নগদ ক্যাশ", "বিকাশ", "নগদ", "ব্যাংক", "কার্ড"
)

fun Double.ifZero(fallback: Double): Double = if (this == 0.0) fallback else this

/**
 * বাজারের ফর্দ শেয়ারিং ফাংশন (WhatsApp, SMS, Messenger ইত্যাদির জন্য)
 */
fun shareMarketListToSocial(context: Context, list: MarketListEntity, items: List<MarketItemEntity>) {
    val sb = StringBuilder()
    sb.append("🛒 *বাজারের ফর্দ: ${list.title}*\n")
    sb.append("📅 তারিখ: ${list.date}\n")
    if (list.shopName.isNotBlank()) {
        sb.append("🏪 দোকান: ${list.shopName}\n")
    }
    sb.append("--------------------------------\n")
    if (items.isEmpty()) {
        sb.append("(কোনো পণ্য তালিকাভুক্ত নেই)\n")
    } else {
        items.forEach { item ->
            val check = if (item.isPurchased) "✓" else "○"
            val price = if (item.isPurchased && item.actualPrice > 0.0) {
                "৳ ${item.actualPrice.toBanglaString()}"
            } else if (item.estimatedPrice > 0.0) {
                "আনুমানিক ৳ ${item.estimatedPrice.toBanglaString()}"
            } else ""
            val priceStr = if (price.isNotBlank()) " ($price)" else ""
            val noteStr = if (item.note.isNotBlank()) " [${item.note}]" else ""
            sb.append("[$check] ${item.name} - ${item.quantity} ${item.unit}$priceStr$noteStr\n")
        }
    }
    sb.append("--------------------------------\n")
    val totalActual = items.filter { it.isPurchased }.sumOf { (if (it.actualPrice > 0.0) it.actualPrice else it.estimatedPrice) * it.quantity.toEnglishDouble().ifZero(1.0) }
    if (list.budget > 0.0) {
        sb.append("💰 বাজেট: ৳ ${list.budget.toBanglaString()}\n")
    }
    sb.append("💵 মোট আসল খরচ: ৳ ${totalActual.toBanglaString()}\n")
    val purchasedCount = items.count { it.isPurchased }
    sb.append("📦 সম্পন্ন: ${purchasedCount.toLong().toBanglaString()}/${items.size.toLong().toBanglaString()} টি\n\n")
    sb.append("— হিসাব নিকাশ অ্যাপ")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "বাজারের ফর্দ শেয়ার করুন")
    context.startActivity(shareIntent)
}

/**
 * ============================================================================
 * বাজার লিস্ট প্রধান স্ক্রিন
 * ============================================================================
 */
@Composable
fun MarketMemoScreen(
    viewModel: TransactionViewModel,
    showAddExternally: Boolean = false,
    onAddDismissed: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val marketLists by viewModel.allMarketLists.collectAsState()
    var selectedList by remember { mutableStateOf<MarketListEntity?>(null) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var showAddItemDialogFromGlobalFab by remember { mutableStateOf(false) }
    var reuseListTarget by remember { mutableStateOf<MarketListEntity?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }

    val activeLists = marketLists.filter { !it.isCompleted }
    val completedLists = marketLists.filter { it.isCompleted }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // গ্লোবাল FAB ক্লিক হ্যান্ডলার
    LaunchedEffect(showAddExternally) {
        if (showAddExternally) {
            if (selectedList == null) {
                showAddListDialog = true
            } else {
                showAddItemDialogFromGlobalFab = true
            }
            onAddDismissed()
        }
    }

    if (selectedList != null) {
        BackHandler { selectedList = null }
        val currentList = marketLists.find { it.id == selectedList!!.id } ?: selectedList!!
        MarketDetailView(
            list = currentList,
            viewModel = viewModel,
            isDark = isDark,
            showAddItemExternal = showAddItemDialogFromGlobalFab,
            onAddItemDismissed = { showAddItemDialogFromGlobalFab = false },
            onBackToLists = { selectedList = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {

            // ১. iOS সেগমেন্টেড কন্ট্রোল
            IOSMarketSegmentedControl(
                items = listOf("চলমান (${activeLists.size.toLong().toBanglaString()})", "সম্পূর্ণ (${completedLists.size.toLong().toBanglaString()})"),
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it },
                activeColor = if (selectedTab == 0) Color(0xFF30D158) else Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ২. iOS সামারি কার্ড
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                glassAlpha = 0.12f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarketSummaryItem(
                        title = "মোট বাজার",
                        value = "${marketLists.size.toLong().toBanglaString()} টি",
                        color = Color(0xFF0A84FF)
                    )
                    MarketSummaryItem(
                        title = "চলমান বাজেট",
                        value = "৳ ${activeLists.sumOf { it.budget }.toBanglaString()}",
                        color = Color(0xFF30D158)
                    )
                    MarketSummaryItem(
                        title = "মোট বাজার খরচ",
                        value = "৳ ${completedLists.sumOf { it.budget.coerceAtLeast(0.0) }.toBanglaString()}",
                        color = Color(0xFFFF9F0A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ৩. লিস্ট কন্টেন্ট
            val currentDisplayLists = if (selectedTab == 0) activeLists else completedLists

            if (currentDisplayLists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateContent(
                        icon = if (selectedTab == 0) Icons.Default.ShoppingCart else Icons.Default.History,
                        title = if (selectedTab == 0) "কোনো চলমান বাজার লিস্ট নেই" else "কোনো সম্পূর্ণ বাজার নেই",
                        subtitle = if (selectedTab == 0) "নিচের + বাটনে ট্যাপ করে নতুন বাজার শুরু করুন" else "বাজার শেষ করলে এখানে সংরক্ষিত থাকবে"
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentDisplayLists, key = { it.id }) { list ->
                        MarketListCard(
                            list = list,
                            viewModel = viewModel,
                            isDark = isDark,
                            onClick = { selectedList = list },
                            onDelete = { viewModel.deleteMarketList(list) },
                            onReuse = { reuseListTarget = list }
                        )
                    }
                }
            }
        }

        // নতুন বাজার তৈরি iOS ডায়ালগ
        if (showAddListDialog) {
            AddOrEditMarketListDialog(
                initialList = null,
                isDark = isDark,
                onDismiss = { showAddListDialog = false },
                onSave = { title, budget, date, shopName ->
                    val newList = MarketListEntity(
                        title = title,
                        date = date,
                        budget = budget,
                        shopName = shopName,
                        isCompleted = false
                    )
                    viewModel.insertMarketList(newList)
                    selectedList = newList
                    showAddListDialog = false
                }
            )
        }

        // আগের বাজার পুনরায় ব্যবহারের iOS ডায়ালগ (Reuse List)
        if (reuseListTarget != null) {
            val target = reuseListTarget!!
            val todayStr = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(Calendar.getInstance().time)
            AddOrEditMarketListDialog(
                initialList = target.copy(
                    title = "${target.title} (নতুন)",
                    date = todayStr
                ),
                isReuse = true,
                isDark = isDark,
                onDismiss = { reuseListTarget = null },
                onSave = { title, budget, date, shopName ->
                    viewModel.reuseShoppingList(
                        originalListId = target.id,
                        newTitle = title,
                        newBudget = budget,
                        newDate = date,
                        newShopName = shopName,
                        onCreated = { createdList ->
                            selectedList = createdList
                            reuseListTarget = null
                        }
                    )
                }
            )
        }
    }
}

/**
 * iOS সেগমেন্টেড কন্ট্রোল
 */
@Composable
fun IOSMarketSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    activeColor: Color = Color(0xFF30D158),
    onItemSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(3.dp)
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) activeColor else Color.Transparent)
                    .clickable { onItemSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MarketSummaryItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = color.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * ============================================================================
 * মার্কেট লিস্ট কার্ড
 * ============================================================================
 */
@Composable
fun MarketListCard(
    list: MarketListEntity,
    viewModel: TransactionViewModel,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onReuse: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.getMarketItems(list.id).collectAsState(initial = emptyList())
    val totalEstimated = items.sumOf { it.estimatedPrice * it.quantity.toEnglishDouble().ifZero(1.0) }
    val totalActual = items.filter { it.isPurchased }.sumOf { (if (it.actualPrice > 0.0) it.actualPrice else it.estimatedPrice) * it.quantity.toEnglishDouble().ifZero(1.0) }
    val purchasedCount = items.count { it.isPurchased }
    val progress = if (items.isNotEmpty()) purchasedCount.toFloat() / items.size else 0f

    val isOverBudget = list.budget > 0.0 && totalActual > list.budget

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = cardBg,
        glassAlpha = if (isDark) 0.12f else 0.05f
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (list.isCompleted) Color(0xFF30D158).copy(alpha = 0.18f)
                                else Color(0xFF0A84FF).copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (list.isCompleted) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = if (list.isCompleted) Color(0xFF30D158) else Color(0xFF0A84FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = list.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(list.date, fontSize = 12.sp, color = subtextColor)
                            if (list.shopName.isNotBlank()) {
                                Text(
                                    text = "• ${list.shopName}",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { shareMarketListToSocial(context, list, items) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার করুন", tint = Color(0xFF30D158), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onReuse, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "কপি করুন",
                            tint = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!list.isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF30D158),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("আইটেম অগ্রগতি", fontSize = 11.sp, color = subtextColor)
                    Text(
                        "${purchasedCount.toLong().toBanglaString()} / ${items.size.toLong().toBanglaString()} কেনা",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (purchasedCount == items.size && items.isNotEmpty()) Color(0xFF30D158) else textColor
                    )
                }

                if (list.budget > 0.0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("বাজেট", fontSize = 11.sp, color = subtextColor)
                        Text("৳ ${list.budget.toBanglaString()}", fontSize = 13.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(if (list.isCompleted) "মোট আসল খরচ" else "আনুমানিক মোট", fontSize = 11.sp, color = subtextColor)
                    Text(
                        text = "৳ ${(if (list.isCompleted) totalActual else totalEstimated).toBanglaString()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) Color(0xFFFF3B30) else Color(0xFF30D158)
                    )
                }
            }

            if (isOverBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "বাজেট ছাড়িয়েছে ৳ ${(totalActual - list.budget).toBanglaString()}",
                        color = Color(0xFFFF3B30),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * ============================================================================
 * বাজার ডিটেইল ভিউ (শেয়ার বাটন ও ক্যাটাগরি গিয়ার আইকন সহ)
 * ============================================================================
 */
@Composable
fun MarketDetailView(
    list: MarketListEntity,
    viewModel: TransactionViewModel,
    isDark: Boolean,
    showAddItemExternal: Boolean = false,
    onAddItemDismissed: () -> Unit = {},
    onBackToLists: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.getMarketItems(list.id).collectAsState(initial = emptyList())
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MarketItemEntity?>(null) }
    var showFinishShoppingDialog by remember { mutableStateOf(false) }
    var showCategoryManagement by remember { mutableStateOf(false) }

    var marketCategories by remember { mutableStateOf(loadMarketCategoriesFromPrefs(context)) }
    var selectedCategoryFilter by remember { mutableStateOf("সব") }

    val totalEstimated = items.sumOf { it.estimatedPrice * it.quantity.toEnglishDouble().ifZero(1.0) }
    val purchasedItems = items.filter { it.isPurchased }
    val totalActual = purchasedItems.sumOf { (if (it.actualPrice > 0.0) it.actualPrice else it.estimatedPrice) * it.quantity.toEnglishDouble().ifZero(1.0) }
    val budget = list.budget
    val isOverBudget = budget > 0.0 && totalActual > budget
    val remainingBudget = budget - totalActual

    val filteredItems = if (selectedCategoryFilter == "সব") items
                        else items.filter { it.category == selectedCategoryFilter }

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)
    val chipUnselectedBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val chipUnselectedText = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF3A3A3C)

    LaunchedEffect(showAddItemExternal) {
        if (showAddItemExternal) {
            showAddItemDialog = true
            onAddItemDismissed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ১. iOS শীর্ষ নেভিগেশন বার
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable(onClick = onBackToLists),
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
                Column {
                    Text(
                        text = list.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(list.date, fontSize = 12.sp, color = subtextColor)
                        if (list.shopName.isNotBlank()) {
                            Text(
                                text = "• 🏪 ${list.shopName}",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // শেয়ার বাটন ও বাজার শেষ করুন বাটন
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158).copy(alpha = 0.15f))
                        .clickable { shareMarketListToSocial(context, list, items) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showFinishShoppingDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (list.isCompleted) Color(0xFF30D158) else Color(0xFF0A84FF)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = if (list.isCompleted) Icons.Default.Check else Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (list.isCompleted) "আপডেট খরচ" else "বাজার শেষ করুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ২. লাইভ হিসাব সামারি কার্ড
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = cardBg,
            glassAlpha = if (isDark) 0.12f else 0.05f
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MarketSummaryItem(
                        title = "বাজেট",
                        value = if (budget > 0.0) "৳ ${budget.toBanglaString()}" else "অনির্দিষ্ট",
                        color = textColor
                    )
                    MarketSummaryItem(
                        title = "মোট আনুমানিক",
                        value = "৳ ${totalEstimated.toBanglaString()}",
                        color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF)
                    )
                    MarketSummaryItem(
                        title = "কেনা হয়েছে (আসল)",
                        value = "৳ ${totalActual.toBanglaString()}",
                        color = Color(0xFF30D158)
                    )
                }

                if (budget > 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isOverBudget) Color(0xFFFF3B30).copy(alpha = 0.12f)
                                else Color(0xFF30D158).copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isOverBudget) Color(0xFFFF3B30) else Color(0xFF30D158),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOverBudget) "বাজেট ছাড়িয়ে গেছে!" else "বাজেটের মধ্যে অবশিষ্ট",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverBudget) Color(0xFFFF3B30) else Color(0xFF30D158)
                            )
                        }
                        Text(
                            text = "৳ ${(if (isOverBudget) (totalActual - budget) else remainingBudget).toBanglaString()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) Color(0xFFFF3B30) else Color(0xFF30D158)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ৩. ক্যাটাগরি ফিল্টার চিপস + লাস্টে গিয়ার আইকন (⚙️)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val allCats = listOf("সব") + marketCategories
            allCats.forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF0A84FF)
                            else chipUnselectedBg
                        )
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else chipUnselectedText
                    )
                }
            }

            // লাস্টে গিয়ার আইকন ⚙️
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable { showCategoryManagement = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Manage Categories",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ৪. পণ্য তালিকা: Apple Reminders ইনসেট গ্রুপড টেবিল
        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateContent(
                    icon = Icons.Default.ShoppingCart,
                    title = if (selectedCategoryFilter == "সব") "ফর্দে কোনো পণ্য নেই" else "এই ক্যাটাগরিতে পণ্য নেই",
                    subtitle = "নিচের + বাটনে ট্যাপ করে বাজারে কেনার মতো পণ্য যোগ করুন"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = cardBg,
                        glassAlpha = if (isDark) 0.12f else 0.05f
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            filteredItems.forEachIndexed { index, item ->
                                MarketItemRow(
                                    item = item,
                                    isDark = isDark,
                                    onTogglePurchased = {
                                        val newStatus = !item.isPurchased
                                        val updatedItem = item.copy(
                                            isPurchased = newStatus,
                                            actualPrice = if (newStatus && item.actualPrice <= 0.0) item.estimatedPrice else item.actualPrice
                                        )
                                        viewModel.updateMarketItem(updatedItem)
                                    },
                                    onEdit = { itemToEdit = item },
                                    onDelete = { viewModel.deleteMarketItem(item) }
                                )
                                if (index < filteredItems.size - 1) {
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

    // পণ্য যোগ করার iOS ডায়ালগ (ক্যালকুলেটর কীবোর্ড সহ)
    if (showAddItemDialog) {
        AddOrEditMarketItemDialog(
            item = null,
            listId = list.id,
            categories = marketCategories,
            isDark = isDark,
            onDismiss = { showAddItemDialog = false },
            onSave = { newItem ->
                viewModel.insertMarketItem(newItem)
                showAddItemDialog = false
            },
            onManageCategories = { showCategoryManagement = true }
        )
    }

    // পণ্য এডিট iOS ডায়ালগ
    if (itemToEdit != null) {
        AddOrEditMarketItemDialog(
            item = itemToEdit,
            listId = list.id,
            categories = marketCategories,
            isDark = isDark,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                viewModel.updateMarketItem(updatedItem)
                itemToEdit = null
            },
            onManageCategories = { showCategoryManagement = true }
        )
    }

    // ক্যাটাগরি ম্যানেজমেন্ট ডায়ালগ
    if (showCategoryManagement) {
        MarketCategoryManagementDialog(
            categories = marketCategories,
            isDark = isDark,
            onDismiss = { showCategoryManagement = false },
            onUpdateCategories = { updated ->
                marketCategories = updated
                saveMarketCategoriesToPrefs(context, updated)
            }
        )
    }

    // বাজার শেষ করার iOS ডায়ালগ
    if (showFinishShoppingDialog) {
        FinishShoppingDialog(
            list = list,
            totalItems = items.size,
            purchasedItemsCount = purchasedItems.size,
            totalAmount = totalActual,
            initialPaymentMethod = list.paymentMethod.ifBlank { "নগদ ক্যাশ" },
            isDark = isDark,
            onDismiss = { showFinishShoppingDialog = false },
            onConfirm = { paymentMethod, createExpense ->
                viewModel.completeShoppingList(
                    list = list,
                    paymentMethod = paymentMethod,
                    createExpense = createExpense,
                    onComplete = {
                        showFinishShoppingDialog = false
                    }
                )
            }
        )
    }
}

/**
 * ============================================================================
 * মার্কেট আইটেম রো (Apple Reminders Style)
 * ============================================================================
 */
@Composable
fun MarketItemRow(
    item: MarketItemEntity,
    isDark: Boolean,
    onTogglePurchased: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val qtyVal = item.quantity.toEnglishDouble().ifZero(1.0)
    val price = if (item.isPurchased && item.actualPrice > 0.0) item.actualPrice else item.estimatedPrice
    val itemTotal = price * qtyVal

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTogglePurchased() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF30D158),
                    uncheckedColor = subtextColor.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isPurchased) subtextColor.copy(alpha = 0.5f) else textColor,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (item.category != "সব" && item.category != "সাধারণ") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.category.substringBefore(" "),
                            fontSize = 10.sp,
                            color = subtextColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        fontSize = 12.sp,
                        color = subtextColor
                    )
                    if (item.isPurchased && item.actualPrice > 0.0) {
                        Text(
                            text = " • আসল দর ৳ ${item.actualPrice.toBanglaString()}",
                            fontSize = 12.sp,
                            color = Color(0xFF30D158),
                            fontWeight = FontWeight.Medium
                        )
                    } else if (item.estimatedPrice > 0.0) {
                        Text(
                            text = " • আনুমানিক ৳ ${item.estimatedPrice.toBanglaString()}",
                            fontSize = 12.sp,
                            color = subtextColor
                        )
                    }
                }

                if (item.note.isNotBlank()) {
                    Text(
                        text = "📝 ${item.note}",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳ ${itemTotal.toBanglaString()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPurchased) Color(0xFF30D158) else textColor
                )
                if (item.isPurchased) {
                    Text("কেনা হয়েছে", fontSize = 10.sp, color = Color(0xFF30D158), fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = subtextColor, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * ============================================================================
 * পিওর iOS স্টাইল ইনপুট ফিল্ড (কোনো বেখাপ্পা আউটলাইন কাটআউট বা ব্যাকগ্রাউন্ড ছাড়া)
 * ============================================================================
 */
@Composable
fun IOSInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    isFocused: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val borderColor = if (isFocused) Color(0xFF30D158) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) Color(0xFF30D158) else hintColor
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = hintColor.copy(alpha = 0.55f), fontSize = 14.sp)
                    }
                    if (readOnly) {
                        Text(value, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(Color(0xFF30D158)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * ============================================================================
 * নতুন বা এডিট বাজার লিস্ট (iOS কার্ড ডায়ালগ + ক্যালকুলেটর কীবোর্ড)
 * ============================================================================
 */
@Composable
fun AddOrEditMarketListDialog(
    initialList: MarketListEntity? = null,
    isReuse: Boolean = false,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, budget: Double, date: String, shopName: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialList?.title ?: "") }
    var budgetText by remember { mutableStateOf(if (initialList != null && initialList.budget > 0) (if (initialList.budget == initialList.budget.toLong().toDouble()) initialList.budget.toLong().toString() else initialList.budget.toString()) else "") }
    var shopName by remember { mutableStateOf(initialList?.shopName ?: "") }

    val todayStr = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD")).format(Calendar.getInstance().time)
    var selectedDate by remember { mutableStateOf(initialList?.date ?: todayStr) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    var showShopManagement by remember { mutableStateOf(false) }

    var shopsList by remember { mutableStateOf(loadMarketShopsFromPrefs(context)) }

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
                        // iOS হেডার
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isReuse) "আগের বাজার থেকে তৈরি" else if (initialList == null) "নতুন বাজার লিস্ট" else "বাজার লিস্ট এডিট",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
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

                        // বাজারের নাম (পিওর iOS ইনপুট)
                        IOSInputField(
                            title = "বাজারের নাম",
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "যেমন: সপ্তাহের বাজার, শুক্রবারের বাজার",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showCalculator = false }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // বাজেট (ক্যালকুলেটর কীবোর্ড ট্রিগার)
                        IOSInputField(
                            title = "আনুমানিক বাজেট (ঐচ্ছিক)",
                            value = if (budgetText.isNotBlank()) "৳ $budgetText" else "",
                            onValueChange = {},
                            placeholder = "টাকা নির্ধারণ করতে ট্যাপ করুন",
                            readOnly = true,
                            isFocused = showCalculator,
                            onClick = { showCalculator = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ক্যালকুলেটর কীবোর্ড
                        if (showCalculator) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔢 ক্যালকুলেটরে হিসাব করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                                Text(
                                    text = "সম্পন্ন ✕",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hintColor,
                                    modifier = Modifier.clickable { showCalculator = false }.padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            CustomCalculatorKeyboard(
                                value = budgetText,
                                onValueChange = { budgetText = it.toEnglishDigits() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // তারিখ নির্বাচন
                        Column {
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
                                    Text(selectedDate, fontSize = 14.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF30D158), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // দোকান নির্বাচন
                        IOSInputField(
                            title = "দোকান / বাজারের নাম (ঐচ্ছিক)",
                            value = shopName,
                            onValueChange = { shopName = it },
                            placeholder = "যেমন: কাঁচাবাজার, স্বপ্ন, আগোরা",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showCalculator = false }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // কুইক শপ সাজেশন চিপস + লাস্টে গিয়ার আইকন ⚙️
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            shopsList.forEach { shop ->
                                val isSelected = shopName == shop
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0A84FF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { shopName = shop }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = shop,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }

                            // গিয়ার আইকন ⚙️
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable { showShopManagement = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Manage Shops",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

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
                                        onSave(title, budgetText.toEnglishDouble(), selectedDate, shopName)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(if (isReuse) "কপি করে শুরু" else "তৈরি করুন", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                    selectedDate = "${cal.get(Calendar.DAY_OF_MONTH).toBanglaString()} ${banglaMonths[cal.get(Calendar.MONTH)]}, ${cal.get(Calendar.YEAR).toBanglaString()}"
                }
                showDatePicker = false
            }
        )
    }

    if (showShopManagement) {
        ShopManagementDialog(
            shops = shopsList,
            isDark = isDark,
            onDismiss = { showShopManagement = false },
            onUpdateShops = { updated ->
                shopsList = updated
                saveMarketShopsToPrefs(context, updated)
            }
        )
    }
}

/**
 * ============================================================================
 * পণ্য যোগ ও এডিট (পিওর iOS স্টাইল + কাস্টম ক্যালকুলেটর কীবোর্ড)
 * ============================================================================
 */
@Composable
fun AddOrEditMarketItemDialog(
    item: MarketItemEntity?,
    listId: String,
    categories: List<String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (MarketItemEntity) -> Unit,
    onManageCategories: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity ?: "১") }
    var unit by remember { mutableStateOf(item?.unit ?: "কেজি") }
    var estPriceText by remember { mutableStateOf(if (item != null && item.estimatedPrice > 0) (if (item.estimatedPrice == item.estimatedPrice.toLong().toDouble()) item.estimatedPrice.toLong().toString() else item.estimatedPrice.toString()) else "") }
    var actPriceText by remember { mutableStateOf(if (item != null && item.actualPrice > 0) (if (item.actualPrice == item.actualPrice.toLong().toDouble()) item.actualPrice.toLong().toString() else item.actualPrice.toString()) else "") }
    var category by remember { mutableStateOf(item?.category ?: categories.firstOrNull() ?: "শাকসবজি 🥬") }
    var note by remember { mutableStateOf(item?.note ?: "") }

    // "est" = আনুমানিক দর, "act" = আসল দর, null = বন্ধ
    var activePriceField by remember { mutableStateOf<String?>(null) }

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
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 16.dp),
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
                            Text(
                                text = if (item == null) "নতুন পণ্য যোগ করুন" else "পণ্য এডিট করুন",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // পণ্যের নাম
                        IOSInputField(
                            title = "পণ্যের নাম",
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "যেমন: দেশি মুরগি, পেঁয়াজ, আলু",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { activePriceField = null }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // পরিমাণ ও একক
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IOSInputField(
                                title = "পরিমাণ",
                                value = quantity,
                                onValueChange = { quantity = it.toEnglishDigits() },
                                placeholder = "১",
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Number,
                                onClick = { activePriceField = null }
                            )
                            IOSInputField(
                                title = "একক",
                                value = unit,
                                onValueChange = { unit = it },
                                placeholder = "কেজি",
                                modifier = Modifier.weight(1f),
                                onClick = { activePriceField = null }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // কুইক একক চিপস
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            MarketUnits.forEach { u ->
                                val isSelected = unit == u
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0A84FF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { unit = u }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = u,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // আনুমানিক দর ও আসল দর (ডিফল্ট কিবোর্ড ডিজেবল, ট্যাপ করলে কাস্টম ক্যালকুলেটর)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IOSInputField(
                                title = "আনুমানিক দর (৳)",
                                value = if (estPriceText.isNotBlank()) "৳ $estPriceText" else "",
                                onValueChange = {},
                                placeholder = "ট্যাপ করুন",
                                readOnly = true,
                                isFocused = activePriceField == "est",
                                onClick = { activePriceField = "est" },
                                modifier = Modifier.weight(1f)
                            )
                            IOSInputField(
                                title = "আসল দর (৳)",
                                value = if (actPriceText.isNotBlank()) "৳ $actPriceText" else "",
                                onValueChange = {},
                                placeholder = "ট্যাপ করুন",
                                readOnly = true,
                                isFocused = activePriceField == "act",
                                onClick = { activePriceField = "act" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // কাস্টম ক্যালকুলেটর কীবোর্ড (টাকার ইনপুটের জন্য)
                        if (activePriceField != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (activePriceField == "est") "🔢 ক্যালকুলেটর: আনুমানিক দর" else "🔢 ক্যালকুলেটর: আসল দর",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158)
                                )
                                Text(
                                    text = "সম্পন্ন ✕",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hintColor,
                                    modifier = Modifier.clickable { activePriceField = null }.padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            CustomCalculatorKeyboard(
                                value = if (activePriceField == "est") estPriceText else actPriceText,
                                onValueChange = { newVal ->
                                    val cleaned = newVal.toEnglishDigits()
                                    if (activePriceField == "est") estPriceText = cleaned
                                    else actPriceText = cleaned
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ক্যাটাগরি চিপস + লাস্টে গিয়ার আইকন ⚙️
                        Text("ক্যাটাগরি:", fontSize = 12.sp, color = hintColor, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0A84FF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { category = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }

                            // গিয়ার আইকন ⚙️
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable { onManageCategories() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Manage Categories",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // বিশেষ নোট
                        IOSInputField(
                            title = "বিশেষ নোট (ঐচ্ছিক)",
                            value = note,
                            onValueChange = { note = it },
                            placeholder = "যেমন: তাজা দেখে আনা, বড় সাইজের",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { activePriceField = null }
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
                                    if (name.isNotBlank()) {
                                        val newItem = item?.copy(
                                            name = name,
                                            quantity = quantity.ifBlank { "১" },
                                            unit = unit.ifBlank { "কেজি" },
                                            estimatedPrice = estPriceText.toEnglishDouble(),
                                            actualPrice = actPriceText.toEnglishDouble(),
                                            category = category,
                                            note = note
                                        ) ?: MarketItemEntity(
                                            listId = listId,
                                            name = name,
                                            quantity = quantity.ifBlank { "১" },
                                            unit = unit.ifBlank { "কেজি" },
                                            estimatedPrice = estPriceText.toEnglishDouble(),
                                            actualPrice = actPriceText.toEnglishDouble(),
                                            category = category,
                                            note = note
                                        )
                                        onSave(newItem)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(if (item == null) "যোগ করুন" else "আপডেট করুন", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
 * "বাজার শেষ করুন" (iOS কনফার্মেশন ও খরচে রূপান্তর ডায়ালগ)
 * ============================================================================
 */
@Composable
fun FinishShoppingDialog(
    list: MarketListEntity,
    totalItems: Int,
    purchasedItemsCount: Int,
    totalAmount: Double,
    initialPaymentMethod: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String, createExpense: Boolean) -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf(initialPaymentMethod) }
    var createExpenseChecked by remember { mutableStateOf(true) }

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
                    modifier = Modifier.fillMaxWidth(0.92f),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("বাজার সম্পন্ন করুন", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = textColor)
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("কেনা পণ্য সংখ্যা:", fontSize = 13.sp, color = hintColor)
                                    Text("${purchasedItemsCount.toLong().toBanglaString()} / ${totalItems.toLong().toBanglaString()} টি", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("মোট আসল খরচ:", fontSize = 13.sp, color = hintColor)
                                    Text("৳ ${totalAmount.toBanglaString()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("পেমেন্ট মাধ্যম:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaymentMethods.forEach { method ->
                                val isSelected = selectedPaymentMethod == method
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0xFF0A84FF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                        )
                                        .clickable { selectedPaymentMethod = method }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = method,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { createExpenseChecked = !createExpenseChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = createExpenseChecked,
                                onCheckedChange = { createExpenseChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF30D158))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "স্বয়ংক্রিয়ভাবে মূল খরচে (🛒 বাজার) হিসেবে যুক্ত করুন",
                                fontSize = 12.sp,
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

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
                                onClick = { onConfirm(selectedPaymentMethod, createExpenseChecked) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("সম্পন্ন করুন", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

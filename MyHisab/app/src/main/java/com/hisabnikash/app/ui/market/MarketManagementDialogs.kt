package com.hisabnikash.app.ui.market

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.ui.components.GlassCard

val InitialDefaultMarketCategories = listOf(
    "শাকসবজি 🥬", "মাছ/মাংস 🐟", "মুদি 🌾", "ফলমূল 🍎", "ডেইরি 🥛", "মসলা 🧂", "গৃহস্থালি 🧼", "অন্যান্য 📦"
)

val InitialDefaultCommonShops = listOf(
    "কাঁচাবাজার", "মুদি দোকান", "সুপারশপ", "স্বপ্ন", "আগোরা", "মীনাবাজার", "মাছের বাজার"
)

fun loadMarketCategoriesFromPrefs(context: Context): List<String> {
    val prefs = context.getSharedPreferences("market_prefs", Context.MODE_PRIVATE)
    val saved = prefs.getString("categories_list", null)
    return if (!saved.isNullOrBlank()) {
        saved.split(";;;").filter { it.isNotBlank() }
    } else {
        InitialDefaultMarketCategories
    }
}

fun saveMarketCategoriesToPrefs(context: Context, list: List<String>) {
    val prefs = context.getSharedPreferences("market_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("categories_list", list.joinToString(";;;")).apply()
}

fun loadMarketShopsFromPrefs(context: Context): List<String> {
    val prefs = context.getSharedPreferences("market_prefs", Context.MODE_PRIVATE)
    val saved = prefs.getString("shops_list", null)
    return if (!saved.isNullOrBlank()) {
        saved.split(";;;").filter { it.isNotBlank() }
    } else {
        InitialDefaultCommonShops
    }
}

fun saveMarketShopsToPrefs(context: Context, list: List<String>) {
    val prefs = context.getSharedPreferences("market_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("shops_list", list.joinToString(";;;")).apply()
}

/**
 * ============================================================================
 * বাজার ক্যাটাগরি ম্যানেজমেন্ট ডায়ালগ (iOS কার্ড স্টাইল)
 * ============================================================================
 */
@Composable
fun MarketCategoryManagementDialog(
    categories: List<String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUpdateCategories: (List<String>) -> Unit
) {
    var categoryList by remember { mutableStateOf(categories) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var inputCategoryName by remember { mutableStateOf("") }

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
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.12f else 0.04f
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // হেডার
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("বাজার ক্যাটাগরি ম্যানেজমেন্ট", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
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

                        // নতুন ক্যাটাগরি যোগ বাটন
                        Button(
                            onClick = {
                                editingIndex = null
                                inputCategoryName = ""
                                showAddOrEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন ক্যাটাগরি যোগ করুন", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ক্যাটাগরি লিস্ট
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(categoryList) { index, cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingIndex = index
                                                inputCategoryName = cat
                                                showAddOrEditDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = hintColor, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (categoryList.size > 1) {
                                                    val updated = categoryList.toMutableList().apply { removeAt(index) }
                                                    categoryList = updated
                                                    onUpdateCategories(updated)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("ঠিক আছে", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddOrEditDialog) {
        Dialog(onDismissRequest = { showAddOrEditDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (editingIndex == null) "নতুন ক্যাটাগরি" else "ক্যাটাগরি সম্পাদনা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputCategoryName,
                        onValueChange = { inputCategoryName = it },
                        placeholder = { Text("যেমন: বেকারি 🍞, ফলমূল 🍎", color = hintColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAddOrEditDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("বাতিল", color = textColor)
                        }
                        Button(
                            onClick = {
                                if (inputCategoryName.isNotBlank()) {
                                    val updated = categoryList.toMutableList()
                                    if (editingIndex != null) {
                                        updated[editingIndex!!] = inputCategoryName
                                    } else {
                                        updated.add(inputCategoryName)
                                    }
                                    categoryList = updated
                                    onUpdateCategories(updated)
                                    showAddOrEditDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(if (editingIndex == null) "যোগ করুন" else "আপডেট", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ============================================================================
 * দোকান / বাজার নাম ম্যানেজমেন্ট ডায়ালগ (iOS কার্ড স্টাইল)
 * ============================================================================
 */
@Composable
fun ShopManagementDialog(
    shops: List<String>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUpdateShops: (List<String>) -> Unit
) {
    var shopList by remember { mutableStateOf(shops) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var inputShopName by remember { mutableStateOf("") }

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
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = cardBg,
                    glassAlpha = if (isDark) 0.12f else 0.04f
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // হেডার
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("দোকান / বাজারের তালিকা", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
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

                        // নতুন দোকান যোগ বাটন
                        Button(
                            onClick = {
                                editingIndex = null
                                inputShopName = ""
                                showAddOrEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন দোকান যোগ করুন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // শপ লিস্ট
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(shopList) { index, shop ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(shop, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingIndex = index
                                                inputShopName = shop
                                                showAddOrEditDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = hintColor, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (shopList.size > 1) {
                                                    val updated = shopList.toMutableList().apply { removeAt(index) }
                                                    shopList = updated
                                                    onUpdateShops(updated)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("ঠিক আছে", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddOrEditDialog) {
        Dialog(onDismissRequest = { showAddOrEditDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = cardBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (editingIndex == null) "নতুন দোকান" else "দোকান সম্পাদনা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputShopName,
                        onValueChange = { inputShopName = it },
                        placeholder = { Text("যেমন: আগোরা, স্বপ্ন, কাঁচাবাজার", color = hintColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAddOrEditDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("বাতিল", color = textColor)
                        }
                        Button(
                            onClick = {
                                if (inputShopName.isNotBlank()) {
                                    val updated = shopList.toMutableList()
                                    if (editingIndex != null) {
                                        updated[editingIndex!!] = inputShopName
                                    } else {
                                        updated.add(inputShopName)
                                    }
                                    shopList = updated
                                    onUpdateShops(updated)
                                    showAddOrEditDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(if (editingIndex == null) "যোগ করুন" else "আপডেট", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

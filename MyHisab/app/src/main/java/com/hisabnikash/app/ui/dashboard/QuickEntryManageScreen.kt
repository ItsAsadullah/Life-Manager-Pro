package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabnikash.app.data.local.QuickEntryEntity
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun QuickEntryManageScreen(
    viewModel: TransactionViewModel,
    categories: List<String>,
    onDismiss: () -> Unit
) {
    val dbQuickEntries by viewModel.allQuickEntries.collectAsState()
    var quickEntries by remember { mutableStateOf(emptyList<QuickEntryEntity>()) }
    
    LaunchedEffect(dbQuickEntries) {
        quickEntries = dbQuickEntries
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<QuickEntryEntity?>(null) }
    
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        quickEntries = quickEntries.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }, onDragEnd = { _, _ ->
        viewModel.updateQuickEntryOrder(quickEntries)
    })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "কুইক এন্ট্রি",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // শীর্ষ + বাটন (নতুন এন্ট্রি যোগ করুন)
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
                                contentDescription = "যোগ করুন",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "নতুন এন্ট্রি",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF0A84FF),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, end = 12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "যোগ করুন",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LiquidBackgroundGlow()
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .reorderable(state),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                items(quickEntries, { it.id }) { entry ->
                    ReorderableItem(state, key = entry.id) { isDragging ->
                        QuickEntryItemCard(
                            entry = entry,
                            onEdit = { entryToEdit = entry },
                            onDelete = { viewModel.deleteQuickEntry(entry) },
                            modifier = Modifier.detectReorderAfterLongPress(state)
                        )
                    }
                }
            }
        }
    }

        if (showAddDialog || entryToEdit != null) {
            AddTransactionDialog(
                categories = categories,
                onManageCategories = {}, // Not strictly needed here, or could be passed down
                onDismiss = { 
                    showAddDialog = false
                    entryToEdit = null
                },
                onSave = { _, _, _, _, _, _, _, _, _ -> }, // Ignored for Quick Entry
                onSaveQuickEntry = { isIncome, amountStr, note, category ->
                    val amount = amountStr.toEnglishDouble()
                    if (amount > 0) {
                        val title = note.ifBlank { category.ifBlank { if (isIncome) "আয়" else "ব্যয়" } }
                        
                        if (entryToEdit != null) {
                            viewModel.updateQuickEntry(
                                entryToEdit!!.copy(
                                    title = title,
                                    amount = amount,
                                    isIncome = isIncome,
                                    category = category
                                )
                            )
                        } else {
                            viewModel.insertQuickEntry(
                                QuickEntryEntity(
                                    title = title,
                                    amount = amount,
                                    isIncome = isIncome,
                                    category = category
                                )
                            )
                        }
                    }
                    showAddDialog = false
                    entryToEdit = null
                },
                initialQuickEntry = entryToEdit
            )
        }
    }
}

@Composable
fun QuickEntryItemCard(
    entry: QuickEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val amountColor = if (entry.isIncome) Color(0xFF34C759) else Color(0xFFFF3B30)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Drag", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontSize = 16.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(amountColor.copy(alpha = 0.15f))
                            .border(1.dp, amountColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (entry.isIncome) "+" else "-",
                            color = amountColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                lineHeight = 12.sp
                            ),
                            modifier = Modifier.offset(y = (-0.5).dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.category,
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Text(
                text = entry.amount.toBanglaString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("এডিট করুন", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("মুছে ফেলুন", color = Color(0xFFFF3B30)) },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

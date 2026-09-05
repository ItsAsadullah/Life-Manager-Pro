package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun CategoryManagementDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onUpdateList: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    
    var localCategories by remember { mutableStateOf(categories) }
    LaunchedEffect(categories) { localCategories = categories }

    val filteredCategories = localCategories.filter { it.contains(searchQuery, ignoreCase = true) }
    
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        if (searchQuery.isEmpty()) {
            localCategories = localCategories.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    }, onDragEnd = { _, _ ->
        if (searchQuery.isEmpty()) {
            onUpdateList(localCategories)
        }
    })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, 
            decorFitsSystemWindows = true 
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 64.dp, end = 16.dp) 
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LiquidBackgroundGlow()
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                
                // Top Bar
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // 🟢 Padding Error Fixed Here 🟢
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.clickable { onDismiss() }.padding(top = 8.dp, bottom = 8.dp, end = 8.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ক্যাটাগরি", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                    // A-Z Sort Icon
                    Icon(Icons.Default.SortByAlpha, contentDescription = "Sort", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, modifier = Modifier.clickable {
                        onUpdateList(categories.sorted())
                    }.padding(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, textStyle = TextStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 16.sp), singleLine = true, modifier = Modifier.weight(1f), decorationBox = { inner -> if (searchQuery.isEmpty()) Text("ক্যাটাগরি খুঁজুন...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp); inner() })
                        if (searchQuery.isNotEmpty()) Icon(Icons.Default.Close, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.clickable { searchQuery = "" })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category List
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier.fillMaxSize().reorderable(state)
                ) {
                    items(filteredCategories, key = { it }) { cat ->
                        ReorderableItem(state, key = cat) { isDragging ->
                            var showMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .then(if (searchQuery.isEmpty()) Modifier.detectReorderAfterLongPress(state) else Modifier)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Menu, contentDescription = "Drag", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(cat, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    
                                    Box {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.clickable { showMenu = true }.padding(4.dp))
                                        
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
                                            DropdownMenuItem(text = { Text("এডিট", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, onClick = { showMenu = false; categoryToEdit = cat })
                                            DropdownMenuItem(text = { Text("ডিলিট", color = Color(0xFFFF3B30)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF3B30)) }, onClick = { showMenu = false; categoryToDelete = cat })
                                            HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(0.1f))
                                            
                                            // Manual Sort Options
                                            DropdownMenuItem(text = { Text("উপরে নিন", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, onClick = { 
                                                showMenu = false
                                                val catIndex = localCategories.indexOf(cat)
                                                if (catIndex > 0) {
                                                    val newList = localCategories.toMutableList()
                                                    val temp = newList[catIndex - 1]
                                                    newList[catIndex - 1] = cat
                                                    newList[catIndex] = temp
                                                    onUpdateList(newList)
                                                }
                                            })
                                            DropdownMenuItem(text = { Text("নিচে নামান", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) }, onClick = { 
                                                showMenu = false
                                                val catIndex = localCategories.indexOf(cat)
                                                if (catIndex < localCategories.size - 1) {
                                                    val newList = localCategories.toMutableList()
                                                    val temp = newList[catIndex + 1]
                                                    newList[catIndex + 1] = cat
                                                    newList[catIndex] = temp
                                                    onUpdateList(newList)
                                                }
                                            })
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
            AddCategoryDialog(onDismiss = { showAddDialog = false }, onSave = { onAddCategory(it) })
        }

        if (categoryToEdit != null) {
            EditCategoryDialog(
                initialName = categoryToEdit!!,
                onDismiss = { categoryToEdit = null },
                onSave = { newName -> onUpdateCategory(categoryToEdit!!, newName) }
            )
        }

        if (categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                title = { Text("ক্যাটাগরি ডিলিট", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                text = { Text("আপনি কি নিশ্চিত যে '${categoryToDelete}' মুছে ফেলতে চান? আগের লেনদেনগুলো 'অন্যান্য' ক্যাটাগরিতে চলে যাবে।", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) },
                confirmButton = { TextButton(onClick = { onDeleteCategory(categoryToDelete!!); categoryToDelete = null }) { Text("ডিলিট", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold) } },
                dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("বাতিল", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) } }
            )
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var catName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.surface).imePadding().padding(24.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("নতুন ক্যাটাগরি", fontSize = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = catName, onValueChange = { catName = it },
                    placeholder = { Text("ক্যাটাগরির নাম", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("বাতিল", color = Color.Gray) }
                    Button(onClick = { if (catName.isNotBlank()) { onSave(catName.trim()); onDismiss() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))) {
                        Text("সেভ করুন", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun EditCategoryDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var catName by remember { mutableStateOf(initialName) }
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.surface).imePadding().padding(24.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ক্যাটাগরি এডিট", fontSize = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = catName, onValueChange = { catName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("বাতিল", color = Color.Gray) }
                    Button(onClick = { if (catName.isNotBlank() && catName != initialName) { onSave(catName.trim()); onDismiss() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))) {
                        Text("আপডেট", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}



package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.data.local.PersonEntity
import com.hisabnikash.app.ui.components.GlassCard

@Composable
fun LendBorrowScreen(
    viewModel: TransactionViewModel,
    onPersonClick: (PersonEntity) -> Unit,
    onTrashClick: () -> Unit = {},
    showAddPersonExternally: Boolean = false,
    onAddPersonDismissed: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var internalShowAddPerson by remember { mutableStateOf(false) }
    val showAddPersonDialog = showAddPersonExternally || internalShowAddPerson
    var personToEdit by remember { mutableStateOf<PersonEntity?>(null) }

    var filterState by remember { mutableStateOf("all") } // "all", "receive", "give"
    
    val persons by viewModel.allPersons.collectAsState()
    val personTransactions by viewModel.allPersonTransactions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val personBalances = remember(persons, personTransactions) {
        persons.associateWith { person ->
            val txs = personTransactions.filter { it.personId == person.id }
            val receive = txs.filter { it.isReceive }.sumOf { it.amount }
            val give = txs.filter { !it.isReceive }.sumOf { it.amount }
            receive - give
        }
    }

    val totalReceive = personBalances.values.filter { it > 0 }.sum()
    val totalGive = kotlin.math.abs(personBalances.values.filter { it < 0 }.sum())

    val filteredPersons = remember(personBalances, filterState, searchQuery) {
        personBalances.filter { (person, balance) ->
            val matchesFilter = when (filterState) {
                "receive" -> balance > 0
                "give" -> balance < 0
                else -> true
            }
            val matchesSearch = person.name.contains(searchQuery, ignoreCase = true) ||
                                person.phone.contains(searchQuery)
            matchesFilter && matchesSearch
        }.keys.toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Row (পাবো, দিবো, মোট)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                glassAlpha = 0.05f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isReceiveSelected = filterState == "receive"
                    val isGiveSelected = filterState == "give"
                    val isAllSelected = filterState == "all"
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isReceiveSelected) IncomeGreen.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                            .clickable { filterState = "receive" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LendBorrowStat(
                            title = "পাবো",
                            amount = "৳ ${totalReceive.toLong().toBanglaString()}",
                            icon = Icons.Default.ArrowDownward,
                            color = IncomeGreen
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isGiveSelected) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { filterState = "give" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LendBorrowStat(
                            title = "দিবো",
                            amount = "৳ ${totalGive.toLong().toBanglaString()}",
                            icon = Icons.Default.ArrowUpward,
                            color = ExpenseRed
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isAllSelected) BalanceBlue.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                            .clickable { filterState = "all" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LendBorrowStat(
                            title = "মোট",
                            amount = "${persons.size.toLong().toBanglaString()} জন",
                            icon = Icons.Default.Person,
                            color = BalanceBlue
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Search Bar with Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    glassAlpha = 0.05f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextWhiteSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("নাম বা ফোন নম্বর দিয়ে খুঁজুন...", color = TextWhiteSecondary, fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextWhiteSecondary)
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("রিপোর্ট ডাউনলোড", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                expanded = false
                                PdfExportUtil.generateSummaryPdf(context, persons, personTransactions)
                            }
                        )
                    }
                }
                
                IconButton(onClick = onTrashClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Trash", tint = TextWhiteSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            if (filteredPersons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateContent(icon = Icons.Default.SyncAlt, title = "কোন ব্যাক্তি নেই", subtitle = "নিচের + বাটনে ক্লিক করে যোগ করুন")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredPersons) { person ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onPersonClick(person) },
                            shape = RoundedCornerShape(16.dp),
                            glassAlpha = 0.05f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val photoUri = person.photoUri
                                if (photoUri != null) {
                                    coil.compose.AsyncImage(
                                        model = photoUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp).clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(BalanceBlue.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = BalanceBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        person.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (person.phone.isNotBlank()) {
                                        Text(person.phone, fontSize = 13.sp, color = TextWhiteSecondary)
                                    }
                                }
                                val balance = personBalances[person] ?: 0.0
                                val balanceColor = if (balance >= 0) IncomeGreen else ExpenseRed
                                Text(
                                    "৳ ${kotlin.math.abs(balance).toLong().toBanglaString()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = balanceColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                var itemMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { itemMenuExpanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = TextWhiteSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = itemMenuExpanded,
                                        onDismissRequest = { itemMenuExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("এডিট প্রোফাইল (Edit Profile)", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                personToEdit = person
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("ডিলিট (Delete)", color = ExpenseRed) },
                                            onClick = {
                                                itemMenuExpanded = false
                                                viewModel.softDeletePerson(person)
                                            }
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

    if (showAddPersonDialog) {
        AddPersonScreen(
            onDismiss = {
                internalShowAddPerson = false
                onAddPersonDismissed()
            },
            onSave = { name, phone, address, date, photoUri ->
                val newPerson = PersonEntity(
                    name = name,
                    phone = phone,
                    address = address,
                    dateAdded = date,
                    photoUri = photoUri
                )
                viewModel.insertPerson(newPerson)
                internalShowAddPerson = false
                onAddPersonDismissed()
            }
        )
    }

    if (personToEdit != null) {
        AddPersonScreen(
            initialPerson = personToEdit,
            onDismiss = { personToEdit = null },
            onSave = { name, phone, address, date, photoUri ->
                val updatedPerson = personToEdit!!.copy(
                    name = name,
                    phone = phone,
                    address = address,
                    photoUri = photoUri ?: personToEdit!!.photoUri
                )
                viewModel.updatePerson(updatedPerson)
                personToEdit = null
            }
        )
    }
}

@Composable
fun LendBorrowStat(title: String, amount: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(amount, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}



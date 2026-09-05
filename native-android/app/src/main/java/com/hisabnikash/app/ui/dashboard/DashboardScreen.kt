package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val Indigo = Color(0xFF4F46E5)
private val Blue = Color(0xFF2563EB)

@Composable
fun DashboardScreen(userId: String, userName: String, onNavigate: (String) -> Unit, onSignOut: () -> Unit) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(userId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppHeader(userName, onSettings = { onNavigate("settings") }) },
        floatingActionButton = {
            Button(
                onClick = { onNavigate("transactions") },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo),
            ) { Text("+  নতুন হিসাব") }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            AppTabs(active = "dashboard", onNavigate = onNavigate, onMore = { onNavigate("more") })
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { BalanceHero(state.income, state.expense, state.meta.borrowed, state.meta.lent) }
                item { OverviewCards(state.meta.noteCount, state.transactions.size) }
                item { ExpenseAnalysis(state.categoryTotals) }
                item { QuickLinks(onNavigate, onSignOut) }
                item { Spacer(Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
fun AppHeader(userName: String, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Hisab Nikash", color = Blue, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text("স্বাগতম, $userName", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onSettings) { Text("⚙ সেটিংস") }
    }
}

@Composable
fun AppTabs(active: String, onNavigate: (String) -> Unit, onMore: () -> Unit) {
    val tabs = listOf("dashboard" to "ড্যাশবোর্ড", "transactions" to "হিসাব", "debts" to "দেনা", "notes" to "নোট", "market" to "বাজার")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (route, label) ->
            TextButton(
                onClick = { onNavigate(route) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (route == active) Blue else Color(0xFFF1F5F9),
                    contentColor = if (route == active) Color.White else Color(0xFF475569),
                ),
                shape = RoundedCornerShape(12.dp),
            ) { Text(label) }
        }
        TextButton(onClick = onMore, shape = RoundedCornerShape(12.dp)) { Text("আরও") }
    }
}

@Composable
private fun BalanceHero(income: Double, expense: Double, borrowed: Double, lent: Double) {
    val month = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale("bn", "BD"))
    Column(
        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Blue, Indigo))).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$month মাসের হিসাব", color = Color(0xFFDCEBFF), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.padding(top = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            AmountBlock("আয়", income, Color(0xFF86EFAC))
            AmountBlock("ব্যয়", expense, Color(0xFFFDA4AF))
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .16f)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AmountBlock("ধার নিয়েছি", borrowed, Color(0xFFFECACA))
            AmountBlock("ধার দিয়েছি", lent, Color(0xFFBBF7D0))
        }
    }
}

@Composable
private fun AmountBlock(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelMedium)
        Text("৳${bnNumber(amount)}", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun OverviewCards(notes: Int, transactions: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(Modifier.weight(1f), "নোট", notes.toString(), Color(0xFF7C3AED))
        StatCard(Modifier.weight(1f), "সাম্প্রতিক হিসাব", transactions.toString(), Blue)
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, tint: Color) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Text(value, color = tint, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExpenseAnalysis(categories: List<Pair<String, Double>>) {
    Card(shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("ব্যয়ের বিশ্লেষণ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (categories.isEmpty()) {
                Text("এ মাসে কোনো ব্যয়ের তথ্য নেই।", Modifier.padding(vertical = 24.dp), color = Color.Gray)
            } else {
                val maximum = categories.maxOf { it.second }.coerceAtLeast(1.0)
                categories.take(6).forEachIndexed { index, (name, amount) ->
                    val color = listOf(Indigo, Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFF43F5E), Color(0xFF8B5CF6))[index % 5]
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, Modifier.width(74.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE2E8F0)))
                        Text("৳${bnNumber(amount)}", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLinks(onNavigate: (String) -> Unit, onSignOut: () -> Unit) {
    Card(shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("দ্রুত কাজ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { onNavigate("transactions") }, modifier = Modifier.fillMaxWidth()) { Text("আয় বা ব্যয় যোগ করুন") }
            TextButton(onClick = { onNavigate("notes") }, modifier = Modifier.fillMaxWidth()) { Text("নতুন নোট লিখুন") }
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("সাইন আউট") }
        }
    }
}

private fun bnNumber(value: Double): String = NumberFormat.getNumberInstance(Locale("bn", "BD")).format(value)

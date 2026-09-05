package com.hisabnikash.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisabnikash.app.data.transactions.Transaction
import com.hisabnikash.app.data.transactions.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionsScreen(userId: String, onBack: () -> Unit) {
    val viewModel: TransactionsViewModel = viewModel(
        key = userId,
        factory = TransactionsViewModel.factory(userId),
    )
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val totalIncome = transactions.filter { it.type == TransactionType.Income }.sumOf(Transaction::amount)
    val totalExpense = transactions.filter { it.type == TransactionType.Expense }.sumOf(Transaction::amount)

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("আয় ও ব্যয়", style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = onBack) { Text("ড্যাশবোর্ড") }
                }
            }
            item { SummaryCard(totalIncome, totalExpense) }
            item {
                TransactionFormCard(
                    form = form,
                    isSaving = isSaving,
                    onUpdate = viewModel::updateForm,
                    onSave = viewModel::save,
                )
            }
            error?.let { message ->
                item {
                    Card { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(message, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearError) { Text("বন্ধ") }
                    } }
                }
            }
            item { Text("সাম্প্রতিক লেনদেন", style = MaterialTheme.typography.titleLarge) }
            if (transactions.isEmpty()) {
                item { Text("এখনও কোনো লেনদেন নেই।") }
            } else {
                items(transactions, key = Transaction::id) { transaction ->
                    TransactionRow(transaction = transaction, onDelete = { viewModel.delete(transaction) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(income: Double, expense: Double) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("আয়: ${money(income)}")
            Text("ব্যয়: ${money(expense)}")
            Text("বর্তমান ব্যালেন্স: ${money(income - expense)}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TransactionFormCard(
    form: TransactionForm,
    isSaving: Boolean,
    onUpdate: ((TransactionForm) -> TransactionForm) -> Unit,
    onSave: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("নতুন লেনদেন", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpdate { it.copy(type = TransactionType.Expense, label = "food") } }) { Text("ব্যয়") }
                TextButton(onClick = { onUpdate { it.copy(type = TransactionType.Income, label = "salary") } }) { Text("আয়") }
            }
            OutlinedTextField(
                value = form.amount,
                onValueChange = { value -> onUpdate { it.copy(amount = value) } },
                label = { Text("পরিমাণ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.label,
                onValueChange = { value -> onUpdate { it.copy(label = value) } },
                label = { Text(if (form.type == TransactionType.Expense) "খাত" else "আয়ের উৎস") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = { value -> onUpdate { it.copy(description = value) } },
                label = { Text("বিবরণ (ঐচ্ছিক)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                if (isSaving) CircularProgressIndicator() else Text("সংরক্ষণ করুন")
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onDelete: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(transaction.label, style = MaterialTheme.typography.titleMedium)
                if (transaction.description.isNotBlank()) Text(transaction.description)
                Text(transaction.date.take(10), style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text(
                    (if (transaction.type == TransactionType.Income) "+" else "-") + money(transaction.amount),
                    color = if (transaction.type == TransactionType.Income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onDelete) { Text("মুছুন") }
            }
        }
    }
}

private fun money(amount: Double): String = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    .format(amount)

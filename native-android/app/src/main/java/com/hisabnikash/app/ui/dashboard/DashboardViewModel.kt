package com.hisabnikash.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.data.dashboard.DashboardMeta
import com.hisabnikash.app.data.dashboard.DashboardRepository
import com.hisabnikash.app.data.transactions.Transaction
import com.hisabnikash.app.data.transactions.TransactionRepository
import com.hisabnikash.app.data.transactions.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val transactions: List<Transaction> = emptyList(),
    val meta: DashboardMeta = DashboardMeta(),
) {
    val income get() = transactions.filter { it.type == TransactionType.Income }.sumOf(Transaction::amount)
    val expense get() = transactions.filter { it.type == TransactionType.Expense }.sumOf(Transaction::amount)
    val categoryTotals get() = transactions.filter { it.type == TransactionType.Expense }
        .groupBy(Transaction::label).mapValues { (_, items) -> items.sumOf(Transaction::amount) }
        .toList().sortedByDescending { it.second }
}

class DashboardViewModel(userId: String) : ViewModel() {
    private val transactions = TransactionRepository()
    private val dashboard = DashboardRepository()
    val state: StateFlow<DashboardUiState> = combine(
        transactions.observeTransactions(userId).catch { emit(emptyList()) },
        dashboard.observeMeta(userId).catch { emit(DashboardMeta()) },
    ) { records, meta -> DashboardUiState(records, meta) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    companion object {
        fun factory(userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) = DashboardViewModel(userId) as T
        }
    }
}

package com.hisabnikash.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.data.transactions.Transaction
import com.hisabnikash.app.data.transactions.TransactionRepository
import com.hisabnikash.app.data.transactions.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionForm(
    val type: TransactionType = TransactionType.Expense,
    val amount: String = "",
    val label: String = "food",
    val description: String = "",
)

class TransactionsViewModel(
    private val userId: String,
    private val repository: TransactionRepository = TransactionRepository(),
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = repository.observeTransactions(userId)
        .catch { _error.value = it.message ?: "লেনদেন লোড করা যায়নি।" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(TransactionForm())
    val form: StateFlow<TransactionForm> = _form.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateForm(transform: (TransactionForm) -> TransactionForm) {
        _form.value = transform(_form.value)
    }

    fun save() = viewModelScope.launch {
        val current = _form.value
        val amount = current.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0 || current.label.isBlank()) {
            _error.value = "সঠিক পরিমাণ ও খাত লিখুন।"
            return@launch
        }
        _isSaving.value = true
        runCatching {
            repository.addTransaction(userId, current.type, amount, current.label.trim(), current.description.trim())
        }.onSuccess {
            _form.value = TransactionForm(type = current.type, label = if (current.type == TransactionType.Expense) "food" else "salary")
        }.onFailure {
            _error.value = it.message ?: "লেনদেন সংরক্ষণ করা যায়নি।"
        }
        _isSaving.value = false
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        runCatching { repository.deleteTransaction(userId, transaction) }
            .onFailure { _error.value = it.message ?: "লেনদেন মুছে ফেলা যায়নি।" }
    }

    fun clearError() { _error.value = null }

    companion object {
        fun factory(userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TransactionsViewModel(userId) as T
        }
    }
}

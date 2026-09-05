package com.hisabnikash.app.data.transactions

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

enum class TransactionType { Expense, Income }

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val label: String,
    val description: String,
    val date: String,
)

class TransactionRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun observeTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        var expenses = emptyList<Transaction>()
        var income = emptyList<Transaction>()

        fun publish() {
            trySend((expenses + income).sortedByDescending(Transaction::date))
        }

        val listeners: List<ListenerRegistration> = listOf(
            firestore.collection("users").document(userId).collection("expenses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    expenses = snapshot?.documents.orEmpty().mapNotNull { document ->
                        val amount = (document.get("amount") as? Number)?.toDouble() ?: return@mapNotNull null
                        Transaction(
                            id = document.id,
                            type = TransactionType.Expense,
                            amount = amount,
                            label = document.getString("category") ?: "other",
                            description = document.getString("description").orEmpty(),
                            date = document.getString("date").orEmpty(),
                        )
                    }
                    publish()
                },
            firestore.collection("users").document(userId).collection("income")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    income = snapshot?.documents.orEmpty().mapNotNull { document ->
                        val amount = (document.get("amount") as? Number)?.toDouble() ?: return@mapNotNull null
                        Transaction(
                            id = document.id,
                            type = TransactionType.Income,
                            amount = amount,
                            label = document.getString("source") ?: "other",
                            description = document.getString("description").orEmpty(),
                            date = document.getString("date").orEmpty(),
                        )
                    }
                    publish()
                },
        )
        awaitClose { listeners.forEach(ListenerRegistration::remove) }
    }

    suspend fun addTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        label: String,
        description: String,
    ) {
        val now = Instant.now().toString()
        val payload = buildMap<String, Any> {
            put("amount", amount)
            put("description", description)
            put("date", now)
            put("createdAt", now)
            when (type) {
                TransactionType.Expense -> {
                    put("category", label)
                    put("paymentMethod", "cash")
                }
                TransactionType.Income -> put("source", label)
            }
        }
        val collection = if (type == TransactionType.Expense) "expenses" else "income"
        firestore.collection("users").document(userId).collection(collection).add(payload).await()
    }

    suspend fun deleteTransaction(userId: String, transaction: Transaction) {
        val collection = if (transaction.type == TransactionType.Expense) "expenses" else "income"
        firestore.collection("users").document(userId).collection(collection).document(transaction.id).delete().await()
    }
}

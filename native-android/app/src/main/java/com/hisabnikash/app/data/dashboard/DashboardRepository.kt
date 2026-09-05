package com.hisabnikash.app.data.dashboard

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DashboardMeta(
    val noteCount: Int = 0,
    val borrowed: Double = 0.0,
    val lent: Double = 0.0,
)

class DashboardRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun observeMeta(userId: String): Flow<DashboardMeta> = callbackFlow {
        var noteCount = 0
        var borrowed = 0.0
        var lent = 0.0
        fun publish() = trySend(DashboardMeta(noteCount, borrowed, lent))
        val listeners: List<ListenerRegistration> = listOf(
            firestore.collection("users").document(userId).collection("notes").addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                noteCount = snapshot?.size() ?: 0
                publish()
            },
            firestore.collection("users").document(userId).collection("debts").addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                borrowed = snapshot?.documents.orEmpty().filter { it.getString("type") == "borrowed" }
                    .sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
                lent = snapshot?.documents.orEmpty().filter { it.getString("type") == "lent" }
                    .sumOf { (it.get("amount") as? Number)?.toDouble() ?: 0.0 }
                publish()
            },
        )
        awaitClose { listeners.forEach(ListenerRegistration::remove) }
    }
}

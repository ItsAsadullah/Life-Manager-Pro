package com.hisabnikash.app.data.notes

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val color: String,
    val createdAt: String,
)

class NotesRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun observeNotes(userId: String): Flow<List<Note>> = callbackFlow {
        val listener = firestore.collection("users").document(userId).collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents.orEmpty().map { document ->
                    Note(
                        id = document.id,
                        title = document.getString("title").orEmpty(),
                        content = document.getString("content").orEmpty(),
                        category = document.getString("category").orEmpty(),
                        color = document.getString("color") ?: "indigo",
                        createdAt = document.getString("createdAt").orEmpty(),
                    )
                }.sortedByDescending(Note::createdAt)
                trySend(notes)
            }
        awaitClose(listener::remove)
    }

    suspend fun saveNote(userId: String, noteId: String?, title: String, content: String, category: String) {
        val reference = firestore.collection("users").document(userId).collection("notes")
        val now = Instant.now().toString()
        if (noteId == null) {
            reference.add(
                mapOf(
                    "title" to title,
                    "content" to content,
                    "type" to "text",
                    "color" to "indigo",
                    "category" to category,
                    "isVoiceNote" to false,
                    "isPinned" to false,
                    "isArchived" to false,
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
            ).await()
        } else {
            reference.document(noteId).update(
                mapOf("title" to title, "content" to content, "category" to category, "updatedAt" to now),
            ).await()
        }
    }

    suspend fun deleteNote(userId: String, noteId: String) {
        firestore.collection("users").document(userId).collection("notes").document(noteId).delete().await()
    }
}

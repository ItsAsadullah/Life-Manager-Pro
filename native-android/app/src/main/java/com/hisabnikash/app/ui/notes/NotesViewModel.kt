package com.hisabnikash.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hisabnikash.app.data.notes.Note
import com.hisabnikash.app.data.notes.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteForm(val id: String? = null, val title: String = "", val content: String = "", val category: String = "")

class NotesViewModel(private val userId: String, private val repository: NotesRepository = NotesRepository()) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.observeNotes(userId).catch { _error.value = "নোট লোড করা যায়নি।" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _form = MutableStateFlow(NoteForm())
    val form = _form.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun update(transform: (NoteForm) -> NoteForm) { _form.value = transform(_form.value) }
    fun edit(note: Note) { _form.value = NoteForm(note.id, note.title, note.content, note.category) }
    fun clearForm() { _form.value = NoteForm() }
    fun clearError() { _error.value = null }

    fun save() = viewModelScope.launch {
        val current = _form.value
        if (current.title.isBlank()) {
            _error.value = "নোটের শিরোনাম দিন।"
            return@launch
        }
        runCatching { repository.saveNote(userId, current.id, current.title.trim(), current.content.trim(), current.category.trim()) }
            .onSuccess { clearForm() }
            .onFailure { _error.value = it.message ?: "নোট সংরক্ষণ করা যায়নি।" }
    }

    fun delete(note: Note) = viewModelScope.launch {
        runCatching { repository.deleteNote(userId, note.id) }
            .onFailure { _error.value = it.message ?: "নোট মুছে ফেলা যায়নি।" }
    }

    companion object {
        fun factory(userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) = NotesViewModel(userId) as T
        }
    }
}

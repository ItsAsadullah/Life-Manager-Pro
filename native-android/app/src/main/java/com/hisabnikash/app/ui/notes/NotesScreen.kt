package com.hisabnikash.app.ui.notes

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisabnikash.app.data.notes.Note

@Composable
fun NotesScreen(userId: String, onBack: () -> Unit) {
    val viewModel: NotesViewModel = viewModel(factory = NotesViewModel.factory(userId))
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("নোট", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onBack) { Text("ড্যাশবোর্ড") }
            } }
            item { NoteEditor(form, viewModel::update, viewModel::save, viewModel::clearForm) }
            error?.let { message -> item { TextButton(onClick = viewModel::clearError) { Text(message) } } }
            item { Text("সব নোট", style = MaterialTheme.typography.titleLarge) }
            if (notes.isEmpty()) item { Text("এখনও কোনো নোট নেই।") }
            items(notes, key = Note::id) { note -> NoteCard(note, onEdit = { viewModel.edit(note) }, onDelete = { viewModel.delete(note) }) }
        }
    }
}

@Composable
private fun NoteEditor(form: NoteForm, onUpdate: ((NoteForm) -> NoteForm) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (form.id == null) "নতুন নোট" else "নোট সম্পাদনা", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(form.title, { value -> onUpdate { it.copy(title = value) } }, Modifier.fillMaxWidth(), label = { Text("শিরোনাম") }, singleLine = true)
            OutlinedTextField(form.content, { value -> onUpdate { it.copy(content = value) } }, Modifier.fillMaxWidth(), label = { Text("বিবরণ") }, minLines = 3)
            OutlinedTextField(form.category, { value -> onUpdate { it.copy(category = value) } }, Modifier.fillMaxWidth(), label = { Text("ক্যাটাগরি (ঐচ্ছিক)") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave) { Text("সংরক্ষণ") }
                if (form.id != null) TextButton(onClick = onCancel) { Text("বাতিল") }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (note.content.isNotBlank()) Text(note.content, modifier = Modifier.padding(top = 5.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (note.category.isNotBlank()) Text(note.category, modifier = Modifier.padding(top = 7.dp), style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("সম্পাদনা") }
                TextButton(onClick = onDelete) { Text("মুছুন") }
            }
        }
    }
}

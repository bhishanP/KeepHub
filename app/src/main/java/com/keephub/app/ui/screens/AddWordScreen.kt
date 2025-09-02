package com.keephub.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    initialTerm: String,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenDuplicate: (Long) -> Unit,
    vm: AddWordViewModel = hiltViewModel()
) {
    LaunchedEffect(initialTerm) { vm.setInitial(initialTerm) }
    val ui by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add word") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding: PaddingValues ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            OutlinedTextField(
                value = ui.termField,
                onValueChange = { s: String -> vm.updateTerm(s) },
                label = { Text("Word") },
                supportingText = { if (ui.duplicateId != null) Text("Looks like a duplicate — open the existing entry below.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.tagsField,
                onValueChange = { s: String -> vm.updateTags(s) },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.notesField,
                onValueChange = { s: String -> vm.updateNotes(s) },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )


            if (ui.duplicateId != null) {
                val existingId = ui.duplicateId!!
                AssistChip(
                    onClick = { onOpenDuplicate(existingId) },
                    label = { Text("Open existing") }
                )
                Text("Looks like a duplicate. Open the existing entry above.")
            }

            Button(
                onClick = { vm.save(onSaved) },
                enabled = ui.canSave && ui.duplicateId == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) { Text("Save") }
        }
    }
}

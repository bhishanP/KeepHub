package com.keephub.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            val tagsRequester = remember { BringIntoViewRequester() }
            val notesRequester = remember { BringIntoViewRequester() }
            val imeVisible = WindowInsets.isImeVisible

            Column(
                Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp) // M3 adaptive constraint
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
            OutlinedTextField(
                value = ui.termField,
                onValueChange = { s: String -> vm.updateTerm(s) },
                label = { Text("Word or phrase") },
                placeholder = { Text("e.g. epiphany, to make, well-being") },
                singleLine = true,
                isError = ui.duplicateWord != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next
                ),
                supportingText = {
                    if (ui.duplicateWord != null) {
                        Text(
                            "This word (or its root) already exists in your vocabulary.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Similar word suggestions while typing
            if (ui.similarWords.isNotEmpty() && ui.duplicateWord == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Similar in your vocabulary:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ui.similarWords.forEach { sim ->
                        AssistChip(
                            onClick = { onOpenDuplicate(sim.id) },
                            label = { Text(sim.term) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = ui.tagsField,
                onValueChange = { s: String -> vm.updateTags(s) },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("e.g. GRE, Academic, Verb") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(tagsRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(150)
                                tagsRequester.bringIntoView()
                            }
                        }
                    }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = ui.notesField,
                onValueChange = { s: String -> vm.updateNotes(s) },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Context, mnemonics, or where you found it...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(notesRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(150)
                                notesRequester.bringIntoView()
                            }
                        }
                    },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                minLines = 3
            )

            // Duplicate Word Banner with "Merge" and "Open" actions
            if (ui.duplicateWord != null) {
                val dup = ui.duplicateWord!!
                Spacer(Modifier.height(16.dp))

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Already in vocabulary: \"${dup.term}\"",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (dup.tags.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Existing tags: ${dup.tags.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        dup.notes?.let { note ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Existing note: $note",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { vm.mergeIntoExisting(onSaved) },
                                enabled = !ui.isMerging,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Merge, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (ui.isMerging) "Merging..." else "Merge notes & tags")
                            }

                            OutlinedButton(
                                onClick = { onOpenDuplicate(dup.id) },
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("View")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.save(onSaved) },
                enabled = ui.canSave && ui.duplicateWord == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Save word")
            }
        }
    }
}
}

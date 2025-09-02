package com.keephub.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.keephub.core.data.db.entity.WordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.keephub.app.nav.Routes
import com.keephub.app.ui.components.EmptyState
import com.keephub.core.data.repo.WordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    navController: NavController,
    vm: WordListViewModel = hiltViewModel()
) {
    val words by vm.items.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KeepHub — Your words") },
                actions = {
                    TextButton(onClick = { navController.navigate(Routes.SETTINGS) }) { Text("Setting") }
                    TextButton(onClick = { navController.navigate(Routes.REVIEW) }) {
                        Text("Review")}
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.size(80.dp), // bigger than default 56.dp
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(36.dp) // default ~24.dp
                )
            }

        }
    ) { padding ->
        if (words.isEmpty()) {
            EmptyState(
                title = "No words yet",
                subtitle = "Add your first word to get started.",
                actionLabel = "Add word",
                onAction = onAdd
            )
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(words, key = { it.id }) { w ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable(onClickLabel = "Open ${w.term}") { onOpen(w.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(w.term, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                            if (w.tags.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(w.tags.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}


@HiltViewModel
class WordListViewModel @Inject constructor(
    repo: WordRepository
) : ViewModel() {
    val items: StateFlow<List<WordEntity>> =
        repo.observeAll()
            .map { it.sortedByDescending { w -> w.updatedAt } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

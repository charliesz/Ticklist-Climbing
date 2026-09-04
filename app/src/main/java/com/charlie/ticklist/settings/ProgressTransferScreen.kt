package com.charlie.ticklist.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charlie.ticklist.data.CollectionEntity
import com.charlie.ticklist.data.ProgressTransferPreview
import com.charlie.ticklist.data.ProgressTransferRepository
import com.charlie.ticklist.data.ProgressTransferState
import com.charlie.ticklist.data.RouteDao
import com.charlie.ticklist.data.CollectionDao
import kotlinx.coroutines.launch

@Composable
fun ProgressTransferScreen(
    collectionDao: CollectionDao,
    routeDao: RouteDao,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val collections by collectionDao
        .observeAllCollections()
        .collectAsState(initial = emptyList())

    var sourceCollection by remember {
        mutableStateOf<CollectionEntity?>(null)
    }

    var targetCollection by remember {
        mutableStateOf<CollectionEntity?>(null)
    }

    var sourceMenuExpanded by remember {
        mutableStateOf(false)
    }

    var targetMenuExpanded by remember {
        mutableStateOf(false)
    }

    var overwriteExistingProgress by remember {
        mutableStateOf(true)
    }

    var transferState by remember {
        mutableStateOf<ProgressTransferState>(
            ProgressTransferState.Idle
        )
    }

    val repository = remember {
        ProgressTransferRepository(
            collectionDao = collectionDao,
            routeDao = routeDao
        )
    }

    LaunchedEffect(collections) {
        if (collections.size >= 2) {
            if (sourceCollection == null) {
                sourceCollection = collections[0]
            }

            if (targetCollection == null) {
                targetCollection = collections[1]
            }
        } else if (collections.size == 1) {
            if (sourceCollection == null) {
                sourceCollection = collections[0]
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack
                ) {
                    Text("Zurück")
                }

                Text(
                    text = "Fortschritt übertragen",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CollectionSelector(
                label = "Von Sammlung",
                selected = sourceCollection,
                collections = collections,
                expanded = sourceMenuExpanded,
                onExpandedChange = {
                    sourceMenuExpanded = it
                },
                onSelected = {
                    sourceCollection = it
                    sourceMenuExpanded = false
                    transferState = ProgressTransferState.Idle
                }
            )

            CollectionSelector(
                label = "Nach Sammlung",
                selected = targetCollection,
                collections = collections,
                expanded = targetMenuExpanded,
                onExpandedChange = {
                    targetMenuExpanded = it
                },
                onSelected = {
                    targetCollection = it
                    targetMenuExpanded = false
                    transferState = ProgressTransferState.Idle
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = overwriteExistingProgress,
                    onClick = {
                        overwriteExistingProgress = true
                    }
                )

                Text("Zielfortschritt überschreiben")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !overwriteExistingProgress,
                    onClick = {
                        overwriteExistingProgress = false
                    }
                )

                Text("Nur leere Zielfelder füllen")
            }

            Button(
                onClick = {
                    val source = sourceCollection
                    val target = targetCollection

                    if (source != null && target != null) {
                        scope.launch {
                            transferState =
                                try {
                                    ProgressTransferState.Preview(
                                        repository.createPreview(
                                            sourceCollectionId = source.id,
                                            targetCollectionId = target.id
                                        )
                                    )
                                } catch (error: Exception) {
                                    ProgressTransferState.Failed(
                                        error.message
                                            ?: "Vorschau konnte nicht erstellt werden."
                                    )
                                }
                        }
                    }
                },
                enabled = sourceCollection != null &&
                        targetCollection != null &&
                        sourceCollection?.id != targetCollection?.id,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Routen prüfen")
            }

            TransferStateContent(
                state = transferState,
                overwriteExistingProgress = overwriteExistingProgress,
                onTransfer = { preview ->
                    scope.launch {
                        transferState =
                            try {
                                ProgressTransferState.Completed(
                                    repository.transferProgress(
                                        sourceCollectionId =
                                            preview.sourceCollectionId,
                                        targetCollectionId =
                                            preview.targetCollectionId,
                                        overwriteExistingProgress =
                                            overwriteExistingProgress
                                    )
                                )
                            } catch (error: Exception) {
                                ProgressTransferState.Failed(
                                    error.message
                                        ?: "Fortschritt konnte nicht übertragen werden."
                                )
                            }
                    }
                },
                onReset = {
                    transferState = ProgressTransferState.Idle
                }
            )
        }
    }
}

@Composable
private fun CollectionSelector(
    label: String,
    selected: CollectionEntity?,
    collections: List<CollectionEntity>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (CollectionEntity) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )

        BoxWithDropdown(
            selectedText = selected?.name ?: "Auswählen",
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            collections.forEach { collection ->
                DropdownMenuItem(
                    text = {
                        Text(collection.name)
                    },
                    onClick = {
                        onSelected(collection)
                    }
                )
            }
        }
    }
}

@Composable
private fun BoxWithDropdown(
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box {
        OutlinedButton(
            onClick = {
                onExpandedChange(true)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedText,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            }
        ) {
            content()
        }
    }
}

@Composable
private fun TransferStateContent(
    state: ProgressTransferState,
    overwriteExistingProgress: Boolean,
    onTransfer: (ProgressTransferPreview) -> Unit,
    onReset: () -> Unit
) {
    when (state) {
        ProgressTransferState.Idle -> {
            Text(
                text = "Wähle eine Quell- und Zielsammlung.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        is ProgressTransferState.Preview -> {
            TransferPreviewCard(
                preview = state.value,
                onTransfer = onTransfer,
                onReset = onReset
            )
        }

        is ProgressTransferState.Completed -> {
            AlertDialog(
                onDismissRequest = onReset,
                title = {
                    Text("Übertragung abgeschlossen")
                },
                text = {
                    Text(
                        "Der Fortschritt wurde von " +
                                "„${state.value.sourceCollectionName}“ nach " +
                                "„${state.value.targetCollectionName}“ " +
                                "übertragen."
                    )
                },
                confirmButton = {
                    Button(onClick = onReset) {
                        Text("OK")
                    }
                }
            )
        }

        is ProgressTransferState.Failed -> {
            AlertDialog(
                onDismissRequest = onReset,
                title = {
                    Text("Übertragung fehlgeschlagen")
                },
                text = {
                    Text(state.message)
                },
                confirmButton = {
                    Button(onClick = onReset) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun TransferPreviewCard(
    preview: ProgressTransferPreview,
    onTransfer: (ProgressTransferPreview) -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Übertragungsprüfung",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Quelle: ${preview.sourceCollectionName}"
            )

            Text(
                text = "Ziel: ${preview.targetCollectionName}"
            )

            Text(
                text = "${preview.sourceRouteCount} Routen in der Quelle"
            )

            Text(
                text = "${preview.targetRouteCount} Routen im Ziel"
            )

            Text(
                text = "${preview.matchingRouteCount} Routen zuordenbar"
            )

            if (preview.canTransfer) {
                Text(
                    text = "Die Übertragung ist möglich.",
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        onTransfer(preview)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fortschritt übertragen")
                }
            } else {
                Text(
                    text = "Die Routenzahlen oder Routennummern " +
                            "stimmen nicht überein.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zurücksetzen")
            }
        }
    }
}

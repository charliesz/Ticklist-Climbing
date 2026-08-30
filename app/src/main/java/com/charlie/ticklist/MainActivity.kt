package com.charlie.ticklist

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charlie.ticklist.data.*
import com.charlie.ticklist.ui.RoutePhotoEditor
import com.charlie.ticklist.ui.theme.TicklistClimbingTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale


private enum class RouteStatus { FLASH, TOP, ZONE, PROJECT }
private enum class StatusFilter { NONE, FLASH, TOP, ZONE, PROJECT }
private enum class SortColumn { ROUTE, FLASH, TOP, ZONE, PROJECT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TicklistClimbingTheme { TicklistApp() } }
    }
}

@Composable
private fun TicklistApp() {
    var collectionId by remember { mutableStateOf<Int?>(null) }

    if (collectionId == null) {
        CollectionsScreen { collectionId = it }
    } else {
        BackHandler { collectionId = null }
        CollectionRoutesScreen(
            collectionId = collectionId!!,
            onBack = { collectionId = null }
        )
    }
}

@Composable
private fun CollectionsScreen(
    onOpen: (Int) -> Unit
) {
    val context = LocalContext.current
    val db = remember { TicklistDatabase.getDatabase(context) }
    val collectionDao = db.collectionDao()
    val routeDao = db.routeDao()
    val scope = rememberCoroutineScope()

    val collections by collectionDao
        .observeAllCollections()
        .collectAsState(emptyList())

    val routes by routeDao
        .observeAllRoutes()
        .collectAsState(emptyList())

    var newDialog by remember { mutableStateOf(false) }
    var editDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CollectionEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newCount by remember { mutableStateOf("90") }

    LaunchedEffect(Unit) {
        if (collectionDao.countCollections() == 0) {
            collectionDao.insertCollection(
                CollectionEntity(
                    id = 1,
                    name = "Boulder 01–90",
                    discipline = "BOULDER",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        if (routeDao.countRoutesForCollection(1) == 0) {
            routeDao.insertRoutes(
                (1..90).map { number ->
                    RouteEntity(
                        number = number,
                        name = "%02d".format(number),
                        difficulty = "",
                        collectionId = 1
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                Text(
                    "Meine Sammlungen",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Kurz tippen zum Öffnen, lange drücken zum Bearbeiten",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newName = ""
                    newCount = "90"
                    newDialog = true
                }
            ) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(collections, key = { it.id }) { collection ->
                val collectionRoutes = routes.filter {
                    it.collectionId == collection.id
                }

                val tops = collectionRoutes.count {
                    it.status == "TOP" || it.status == "FLASH"
                }

                val flashes = collectionRoutes.count {
                    it.status == "FLASH"
                }

                Card(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                onOpen(collection.id)
                            },
                            onLongClick = {
                                editing = collection
                                name = collection.name
                                editDialog = true
                            }
                        )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            collection.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            "${collectionRoutes.size} Routen · " +
                                    "$tops Top ($flashes Flash)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (newDialog) {
        AlertDialog(
            onDismissRequest = {
                newDialog = false
            },
            title = {
                Text("Neue Sammlung")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                        },
                        label = {
                            Text("Name")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newCount,
                        onValueChange = {
                            newCount = it
                        },
                        label = {
                            Text("Anzahl Routen")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val collectionName = newName.trim()
                        val count = newCount.toIntOrNull() ?: 0

                        if (collectionName.isNotBlank()) {
                            scope.launch {
                                val id = collectionDao
                                    .insertCollection(
                                        CollectionEntity(
                                            name = collectionName,
                                            discipline = "BOULDER",
                                            createdAt =
                                                System.currentTimeMillis()
                                        )
                                    )
                                    .toInt()

                                if (count > 0) {
                                    routeDao.insertRoutes(
                                        (1..count).map { number ->
                                            RouteEntity(
                                                number = number,
                                                name = "%02d".format(number),
                                                difficulty = "",
                                                collectionId = id
                                            )
                                        }
                                    )
                                }

                                newDialog = false
                            }
                        }
                    }
                ) {
                    Text("Anlegen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newDialog = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (editDialog) {
        AlertDialog(
            onDismissRequest = {
                editDialog = false
                editing = null
            },
            title = {
                Text("Sammlung bearbeiten")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = {
                            Text("Name")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    TextButton(
                        onClick = {
                            deleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sammlung löschen")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val collection = editing
                        val newCollectionName = name.trim()

                        if (
                            collection != null &&
                            newCollectionName.isNotBlank()
                        ) {
                            scope.launch {
                                collectionDao.updateCollectionName(
                                    collection.id,
                                    newCollectionName
                                )

                                editDialog = false
                                editing = null
                            }
                        }
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editDialog = false
                        editing = null
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = {
                deleteDialog = false
            },
            title = {
                Text("Sammlung löschen?")
            },
            text = {
                Text(
                    "Die Sammlung und alle zugehörigen Routen werden dauerhaft gelöscht."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val collection = editing

                        if (collection != null) {
                            scope.launch {
                                routeDao.deleteRoutesForCollection(
                                    collection.id
                                )
                                collectionDao.deleteCollectionById(
                                    collection.id
                                )

                                deleteDialog = false
                                editDialog = false
                                editing = null
                            }
                        }
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteDialog = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun CollectionRoutesScreen(
    collectionId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember {
        TicklistDatabase.getDatabase(context)
    }

    val routeDao = db.routeDao()
    val collectionDao = db.collectionDao()
    val photoDao = db.routePhotoDao()
    val photoRepository = remember {
        RoutePhotoRepository(context, photoDao)
    }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val routes by routeDao
        .observeRoutesForCollection(collectionId)
        .collectAsState(emptyList())
    val mainPhotos by photoDao
        .observeMainPhotosForCollection(collectionId)
        .collectAsState(emptyList())

    val mainPhotoByRouteId = mainPhotos.associateBy {
        it.routeId
    }

    var collectionName by remember { mutableStateOf("") }

    LaunchedEffect(collectionId) {
        collectionName =
            collectionDao.getCollection(collectionId)?.name ?: ""
    }

    val allFilters = remember {
        setOf(
            StatusFilter.NONE,
            StatusFilter.FLASH,
            StatusFilter.TOP,
            StatusFilter.ZONE,
            StatusFilter.PROJECT
        )
    }

    var filters by remember { mutableStateOf(allFilters) }
    var sort by remember { mutableStateOf(SortColumn.ROUTE) }
    var ascending by remember { mutableStateOf(true) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var filterOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var bulk by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }

    var editingNumber by remember { mutableStateOf<Int?>(null) }
    var name by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<RouteStatus?>(null) }
    var statusAt by remember { mutableStateOf<Long?>(null) }
    var completed by remember { mutableStateOf<Long?>(null) }

    var bulkStatusEnabled by remember { mutableStateOf(false) }
    var bulkStatus by remember { mutableStateOf<RouteStatus?>(null) }
    var bulkDateEnabled by remember { mutableStateOf(false) }
    var bulkDate by remember { mutableStateOf<Long?>(null) }

    val shown = routes
        .filter {
            routeFilter(it.status) in filters
        }
        .sortedWith(
            routeComparator(
                sort,
                ascending
            )
        )

    val tops = routes.count {
        it.status == "TOP" || it.status == "FLASH"
    }

    val flashes = routes.count {
        it.status == "FLASH"
    }

    val zones = routes.count {
        it.status == "ZONE"
    }

    fun openRoute(route: RouteEntity) {
        scope.launch {
            val current = routeDao.getRoute(route.number) ?: route

            editingNumber = current.number
            name = current.name
            difficulty = current.difficulty
            status = current.status.toRouteStatus()
            statusAt = current.statusChangedAt
            completed = current.completedDate
            dialog = true
        }
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                if (selectionMode) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text("${selected.size} ausgewählt")

                        Row {
                            TextButton(
                                onClick = {
                                    if (selected.isNotEmpty()) {
                                        bulkStatusEnabled = false
                                        bulkDateEnabled = false
                                        bulkStatus = null
                                        bulkDate = null
                                        bulk = true
                                    }
                                }
                            ) {
                                Text("Bearbeiten")
                            }

                            TextButton(
                                onClick = {
                                    selected = emptySet()
                                    selectionMode = false
                                }
                            ) {
                                Text("Fertig")
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                collectionName,
                                style =
                                    MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${shown.size} von ${routes.size} Routen",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(onClick = onBack) {
                            Text("Sammlungen")
                        }
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectionMode = true
                                selected = emptySet()
                            },
                            contentPadding = PaddingValues(
                                horizontal = 10.dp,
                                vertical = 2.dp
                            )
                        ) {
                            Text(
                                "Bearbeiten",
                                fontSize = 11.sp
                            )
                        }

                        Box {
                            OutlinedButton(
                                onClick = {
                                    filterOpen = true
                                },
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 2.dp
                                )
                            ) {
                                Text(
                                    "Filter",
                                    fontSize = 11.sp
                                )
                            }

                            FilterMenu(
                                filterOpen,
                                filters,
                                allFilters,
                                { filterOpen = false }
                            ) {
                                filters = it
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            CalculationBar(
                tops,
                flashes,
                zones,
                Modifier.navigationBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNumber = null
                    name = ""
                    difficulty = ""
                    status = null
                    statusAt = null
                    completed = null
                    dialog = true
                }
            ) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                RouteHeader(sort, ascending) {
                    if (sort == it) {
                        ascending = !ascending
                    } else {
                        sort = it
                        ascending = true
                    }
                }
            }

            items(
                shown,
                key = { it.id }
            ) { route ->
                RouteRow(
                    route = route,
                    mainPhoto = mainPhotoByRouteId[route.id],
                    selectionMode = selectionMode,
                    selected = route.number in selected,
                    onSelectedChange = {
                        selected =
                            if (route.number in selected) {
                                selected - route.number
                            } else {
                                selected + route.number
                            }
                    },
                    onStatusChange = { newStatus ->
                        val now = System.currentTimeMillis()

                        scope.launch {
                            routeDao.updateRouteWithDates(
                                number = route.number,
                                name = route.name,
                                difficulty = route.difficulty,
                                status = newStatus.name,
                                statusChangedAt = now,
                                completedDate =
                                    if (
                                        newStatus == RouteStatus.TOP ||
                                        newStatus == RouteStatus.FLASH
                                    ) {
                                        route.completedDate ?: now
                                    } else {
                                        null
                                    },
                                collectionId = collectionId
                            )

                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                        }
                    },
                    onEdit = {
                        openRoute(route)
                    }
                )
            }
        }
    }

    if (dialog) {
        SingleRouteDialog(
            context = context,
            route = routes.firstOrNull {
                it.number == editingNumber
            },
            photoDao = photoDao,
            photoRepository = photoRepository,
            title = if (editingNumber == null) {
                "Route hinzufügen"
            } else {
                "Route bearbeiten"
            },
            name = name,
            difficulty = difficulty,
            status = status,
            statusChangedAt = statusAt,
            completedDate = completed,
            onNameChanged = {
                name = it
            },
            onDifficultyChanged = {
                difficulty = it
            },
            onStatusChanged = {
                status = it
            },
            onCompletedDateChanged = {
                completed = it
            },
            onDelete = if (editingNumber != null) {
                {
                    delete = true
                }
            } else {
                null
            },
            onDismiss = {
                dialog = false
                editingNumber = null
            },
            onSave = {
                if (name.isNotBlank()) {
                    scope.launch {
                        val now = System.currentTimeMillis()

                        val old = routes.firstOrNull {
                            it.number == editingNumber
                        }

                        val savedStatusAt =
                            old?.statusChangedAt
                                ?: statusAt
                                ?: now

                        val savedCompleted =
                            if (
                                status == RouteStatus.TOP ||
                                status == RouteStatus.FLASH
                            ) {
                                completed
                            } else {
                                null
                            }

                        if (editingNumber == null) {
                            routeDao.insertRoute(
                                RouteEntity(
                                    number = (
                                            routes.maxOfOrNull { it.number }?.plus(1) ?: 1
                                            ),
                                    name = name,
                                    difficulty = difficulty,
                                    status = status?.name,
                                    statusChangedAt = savedStatusAt,
                                    completedDate = savedCompleted,
                                    collectionId = collectionId
                                )
                            )
                        } else {
                            routeDao.updateRouteWithDates(
                                number = editingNumber!!,
                                name = name,
                                difficulty = difficulty,
                                status = status?.name,
                                statusChangedAt = savedStatusAt,
                                completedDate = savedCompleted,
                                collectionId = collectionId
                            )
                        }

                        dialog = false
                        editingNumber = null
                    }
                }
            }
        )
    }

    if (bulk) {
        BulkEditDialog(
            context = context,
            selectedCount = selected.size,
            statusEnabled = bulkStatusEnabled,
            status = bulkStatus,
            dateEnabled = bulkDateEnabled,
            date = bulkDate,
            onStatusEnabledChanged = {
                bulkStatusEnabled = it
            },
            onStatusChanged = {
                bulkStatus = it
            },
            onDateEnabledChanged = {
                bulkDateEnabled = it
            },
            onDateChanged = {
                bulkDate = it
            },
            onDismiss = {
                bulk = false
            },
            onSave = {
                scope.launch {
                    val now = System.currentTimeMillis()

                    routes
                        .filter {
                            it.number in selected
                        }
                        .forEach { route ->
                            routeDao.updateRouteWithDates(
                                number = route.number,
                                name = route.name,
                                difficulty = route.difficulty,
                                status = if (bulkStatusEnabled) {
                                    bulkStatus?.name
                                } else {
                                    route.status
                                },
                                statusChangedAt = if (
                                    bulkStatusEnabled
                                ) {
                                    now
                                } else {
                                    route.statusChangedAt
                                },
                                completedDate = if (
                                    bulkDateEnabled
                                ) {
                                    bulkDate
                                } else {
                                    route.completedDate
                                },
                                collectionId = collectionId
                            )
                        }

                    selected = emptySet()
                    selectionMode = false
                    bulk = false
                }
            }
        )
    }

    if (delete) {
        AlertDialog(
            onDismissRequest = {
                delete = false
            },
            title = {
                Text("Route löschen?")
            },
            text = {
                Text("Diese Route wird dauerhaft gelöscht.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val number = editingNumber

                        if (number != null) {
                            routes.firstOrNull {
                                it.number == number
                            }?.let { route ->
                                scope.launch {
                                    routeDao.deleteRoute(route)
                                }
                            }
                        }

                        delete = false
                        dialog = false
                        editingNumber = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        delete = false
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun FilterMenu(
    expanded: Boolean,
    selectedFilters: Set<StatusFilter>,
    allFilters: Set<StatusFilter>,
    onDismiss: () -> Unit,
    onChanged: (Set<StatusFilter>) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        FilterItem("Flash", StatusFilter.FLASH in selectedFilters) {
            onChanged(
                selectedFilters.toggleFilter(StatusFilter.FLASH)
            )
        }

        FilterItem("Top", StatusFilter.TOP in selectedFilters) {
            onChanged(
                selectedFilters.toggleFilter(StatusFilter.TOP)
            )
        }

        FilterItem("Zone", StatusFilter.ZONE in selectedFilters) {
            onChanged(
                selectedFilters.toggleFilter(StatusFilter.ZONE)
            )
        }

        FilterItem(
            "Projekt",
            StatusFilter.PROJECT in selectedFilters
        ) {
            onChanged(
                selectedFilters.toggleFilter(StatusFilter.PROJECT)
            )
        }

        FilterItem("Ohne", StatusFilter.NONE in selectedFilters) {
            onChanged(
                selectedFilters.toggleFilter(StatusFilter.NONE)
            )
        }

        DropdownMenuItem(
            text = {
                Text("Alle")
            },
            onClick = {
                onChanged(allFilters)
            }
        )

        DropdownMenuItem(
            text = {
                Text("Keine")
            },
            onClick = {
                onChanged(emptySet())
            }
        )

        DropdownMenuItem(
            text = {
                Text("Abgeschlossene aus")
            },
            onClick = {
                onChanged(
                    setOf(
                        StatusFilter.NONE,
                        StatusFilter.ZONE,
                        StatusFilter.PROJECT
                    )
                )
            }
        )
    }
}

@Composable
private fun FilterItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(label)
        },
        onClick = onClick,
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    onClick()
                }
            )
        }
    )
}

@Composable
private fun SingleRouteDialog(
    context: Context,
    route: RouteEntity?,
    photoDao: RoutePhotoDao,
    photoRepository: RoutePhotoRepository,
    title: String,
    name: String,
    difficulty: String,
    status: RouteStatus?,
    statusChangedAt: Long?,
    completedDate: Long?,
    onNameChanged: (String) -> Unit,
    onDifficultyChanged: (String) -> Unit,
    onStatusChanged: (RouteStatus?) -> Unit,
    onCompletedDateChanged: (Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = {
                        Text("Name oder Nummer")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = difficulty,
                    onValueChange = onDifficultyChanged,
                    label = {
                        Text("Schwierigkeit")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (route != null) {
                    RoutePhotoEditor(
                        route = route,
                        photoDao = photoDao,
                        photoRepository = photoRepository,
                        onLongPressPhoto = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Speichere die Route zuerst, um Fotos hinzuzufügen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium
                )

                StatusChoice("Kein Status", status == null) {
                    onStatusChanged(null)
                }

                StatusChoice("Flash", status == RouteStatus.FLASH) {
                    onStatusChanged(RouteStatus.FLASH)
                }

                StatusChoice("Top", status == RouteStatus.TOP) {
                    onStatusChanged(RouteStatus.TOP)
                }

                StatusChoice("Zone", status == RouteStatus.ZONE) {
                    onStatusChanged(RouteStatus.ZONE)
                }

                StatusChoice("Projekt", status == RouteStatus.PROJECT) {
                    onStatusChanged(RouteStatus.PROJECT)
                }

                if (statusChangedAt != null) {
                    Text(
                        text = "Status eingetragen: ${
                            formatDateTime(statusChangedAt)
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (
                    status == RouteStatus.TOP ||
                    status == RouteStatus.FLASH
                ) {
                    Text(
                        text = "Erfolgsdatum: ${
                            completedDate?.let {
                                formatDate(it)
                            } ?: "nicht eingetragen"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context = context,
                                currentDate = completedDate,
                                onDateSelected = onCompletedDateChanged
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (completedDate == null) {
                                "Erfolgsdatum wählen"
                            } else {
                                "Erfolgsdatum ändern"
                            }
                        )
                    }

                    if (completedDate != null) {
                        TextButton(
                            onClick = {
                                onCompletedDateChanged(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Erfolgsdatum löschen")
                        }
                    }
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Route löschen")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun BulkEditDialog(
    context: Context,
    selectedCount: Int,
    statusEnabled: Boolean,
    status: RouteStatus?,
    dateEnabled: Boolean,
    date: Long?,
    onStatusEnabledChanged: (Boolean) -> Unit,
    onStatusChanged: (RouteStatus?) -> Unit,
    onDateEnabledChanged: (Boolean) -> Unit,
    onDateChanged: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("$selectedCount Routen bearbeiten")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = statusEnabled,
                        onCheckedChange = onStatusEnabledChanged
                    )

                    Text("Status ändern")
                }

                if (statusEnabled) {
                    StatusChoice("Kein Status", status == null) {
                        onStatusChanged(null)
                    }

                    StatusChoice("Flash", status == RouteStatus.FLASH) {
                        onStatusChanged(RouteStatus.FLASH)
                    }

                    StatusChoice("Top", status == RouteStatus.TOP) {
                        onStatusChanged(RouteStatus.TOP)
                    }

                    StatusChoice("Zone", status == RouteStatus.ZONE) {
                        onStatusChanged(RouteStatus.ZONE)
                    }

                    StatusChoice("Projekt", status == RouteStatus.PROJECT) {
                        onStatusChanged(RouteStatus.PROJECT)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dateEnabled,
                        onCheckedChange = onDateEnabledChanged
                    )

                    Text("Erfolgsdatum ändern")
                }

                if (dateEnabled) {
                    Text(
                        text = "Datum: ${
                            date?.let {
                                formatDate(it)
                            } ?: "nicht gesetzt"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context = context,
                                currentDate = date,
                                onDateSelected = onDateChanged
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Datum auswählen")
                    }

                    if (date != null) {
                        TextButton(
                            onClick = {
                                onDateChanged(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Datum löschen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text("Auf $selectedCount anwenden")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun StatusChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }
    }
}

@Composable
private fun CalculationBar(
    topCount: Int,
    flashCount: Int,
    zoneCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Text(
            text = "Top: $topCount ($flashCount Flash) · Zone: $zoneCount",
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun RouteHeader(
    column: SortColumn,
    ascending: Boolean,
    onSort: (SortColumn) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        listOf(
            "Route" to SortColumn.ROUTE,
            "Flash" to SortColumn.FLASH,
            "Top" to SortColumn.TOP,
            "Zone" to SortColumn.ZONE,
            "Projekt" to SortColumn.PROJECT
        ).forEach { (label, sortColumn) ->
            TextButton(
                onClick = {
                    onSort(sortColumn)
                },
                modifier = Modifier.weight(
                    if (sortColumn == SortColumn.ROUTE) {
                        1.15f
                    } else {
                        1f
                    }
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                val arrow =
                    if (sortColumn == column) {
                        if (ascending) " ↑" else " ↓"
                    } else {
                        ""
                    }

                Text(
                    text = label + arrow,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RouteRow(
    route: RouteEntity,
    mainPhoto: RoutePhotoEntity?,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectedChange: () -> Unit,
    onStatusChange: (RouteStatus) -> Unit,
    onEdit: () -> Unit
) {
    var rowProgress by remember(route.id) {
        mutableStateOf(0f)
    }

    val rowBackground =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = {
                            onSelectedChange()
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .pointerInput(route.id, selectionMode) {
                            detectTapGestures(
                                onTap = {
                                    if (selectionMode) {
                                        onSelectedChange()
                                    }
                                },
                                onLongPress = {
                                    if (!selectionMode) {
                                        onEdit()
                                    }
                                }
                            )
                        }
                ) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (route.difficulty.isNotBlank()) {
                        Text(
                            text = route.difficulty,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (mainPhoto != null) {
                    RouteRowThumbnail(
                        photo = mainPhoto,
                        onClick = {
                            onEdit()
                        },
                        onLongClick = {
                            onEdit()
                        }
                    )
                }

                StatusButton(
                    label = "Flash",
                    selected = route.status == "FLASH",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.FLASH)
                    }
                )

                StatusButton(
                    label = "Top",
                    selected = route.status == "TOP",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.TOP)
                    }
                )

                StatusButton(
                    label = "Zone",
                    selected = route.status == "ZONE",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.ZONE)
                    }
                )

                StatusButton(
                    label = "Projekt",
                    selected = route.status == "PROJECT",
                    modifier = Modifier.weight(1f),
                    onProgress = {
                        rowProgress = it
                    },
                    onComplete = {
                        rowProgress = 0f
                        onStatusChange(RouteStatus.PROJECT)
                    }
                )
            }
        }

        if (rowProgress > 0f) {
            BorderProgress(
                progress = rowProgress,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
private fun RouteRowThumbnail(
    photo: RoutePhotoEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val imageBitmap = remember(photo.filePath) {
        val file = java.io.File(photo.filePath)

        if (file.exists()) {
            runCatching {
                android.graphics.BitmapFactory
                    .decodeFile(file.absolutePath)
                    ?.asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = {
                        onClick()
                    },
                    onLongPress = {
                        onLongClick()
                    }
                )
            }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Hauptfoto der Route",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "–",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun StatusButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit
) {
    val animation = remember {
        Animatable(0f)
    }

    Box(
        modifier
            .height(44.dp)
            .padding(1.dp)
    ) {
        if (selected) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
            ) {
                Text(label, fontSize = 10.sp)
            }
        } else {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
            ) {
                Text(label, fontSize = 10.sp)
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(label) {
                    detectTapGestures(
                        onPress = {
                            kotlinx.coroutines.coroutineScope {
                                var completed = false

                                animation.snapTo(0f)
                                onProgress(0f)

                                val job = launch {
                                    animation.animateTo(
                                        1f,
                                        tween(1500)
                                    ) {
                                        onProgress(value)
                                    }

                                    completed = true
                                }

                                val released = tryAwaitRelease()

                                if (completed && released) {
                                    onComplete()
                                } else {
                                    job.cancel()
                                    animation.snapTo(0f)
                                    onProgress(0f)
                                }

                                animation.snapTo(0f)
                                onProgress(0f)
                            }
                        }
                    )
                }
        )
    }
}

@Composable
private fun BorderProgress(
    progress: Float,
    color: Color
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(1.dp)
    ) {
        val stroke = 4.dp.toPx()
        val path = AndroidPath()

        path.addRoundRect(
            RectF(
                stroke / 2f,
                stroke / 2f,
                size.width - stroke / 2f,
                size.height - stroke / 2f
            ),
            12.dp.toPx(),
            12.dp.toPx(),
            AndroidPath.Direction.CW
        )

        val measure = PathMeasure(path, false)
        val part = AndroidPath()

        measure.getSegment(
            0f,
            measure.length * progress.coerceIn(0f, 1f),
            part,
            true
        )

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = stroke
            this.color = color.toArgb()
        }

        drawIntoCanvas {
            it.nativeCanvas.drawPath(part, paint)
        }
    }
}

private fun routeFilter(
    status: String?
): StatusFilter? {
    return when (status) {
        null -> StatusFilter.NONE
        "FLASH" -> StatusFilter.FLASH
        "TOP" -> StatusFilter.TOP
        "ZONE" -> StatusFilter.ZONE
        "PROJECT" -> StatusFilter.PROJECT
        else -> null
    }
}

private fun routeComparator(
    column: SortColumn,
    ascending: Boolean
): Comparator<RouteEntity> {
    val comparator = when (column) {
        SortColumn.ROUTE -> compareBy<RouteEntity> {
            it.number
        }

        SortColumn.FLASH -> compareByDescending<RouteEntity> {
            it.status == "FLASH"
        }.thenBy {
            it.number
        }

        SortColumn.TOP -> compareByDescending<RouteEntity> {
            it.status == "TOP"
        }.thenBy {
            it.number
        }

        SortColumn.ZONE -> compareByDescending<RouteEntity> {
            it.status == "ZONE"
        }.thenBy {
            it.number
        }

        SortColumn.PROJECT -> compareByDescending<RouteEntity> {
            it.status == "PROJECT"
        }.thenBy {
            it.number
        }
    }

    return if (ascending) {
        comparator
    } else {
        comparator.reversed()
    }
}

private fun Set<StatusFilter>.toggleFilter(
    filter: StatusFilter
): Set<StatusFilter> {
    return if (filter in this) {
        this - filter
    } else {
        this + filter
    }
}

private fun String?.toRouteStatus(): RouteStatus? {
    return this?.let {
        runCatching {
            RouteStatus.valueOf(it)
        }.getOrNull()
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd.MM.yyyy",
        Locale.getDefault()
    ).format(Date(timestamp))
}

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat(
        "dd.MM.yyyy, HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}

private fun showDatePicker(
    context: Context,
    currentDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val calendar = Calendar.getInstance()

    if (currentDate != null) {
        calendar.timeInMillis = currentDate
    }

    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(
                    Calendar.YEAR,
                    year
                )
                set(
                    Calendar.MONTH,
                    month
                )
                set(
                    Calendar.DAY_OF_MONTH,
                    day
                )
                set(
                    Calendar.HOUR_OF_DAY,
                    12
                )
                set(
                    Calendar.MINUTE,
                    0
                )
                set(
                    Calendar.SECOND,
                    0
                )
                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

            onDateSelected(selectedDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

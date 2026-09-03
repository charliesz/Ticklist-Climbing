package com.charlie.ticklist

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charlie.ticklist.data.CollectionEntity
import com.charlie.ticklist.data.CollectionPhotoStorage
import com.charlie.ticklist.data.PhotoStorage
import com.charlie.ticklist.data.RouteEntity
import com.charlie.ticklist.data.RoutePhotoDao
import com.charlie.ticklist.data.RoutePhotoEntity
import com.charlie.ticklist.data.RoutePhotoRepository
import com.charlie.ticklist.data.TicklistDatabase
import com.charlie.ticklist.data.rememberPhotoPicker
import com.charlie.ticklist.settings.AppSettings
import com.charlie.ticklist.settings.AppSettingsRepository
import com.charlie.ticklist.settings.SettingsScreen
import com.charlie.ticklist.ui.PhotoViewerDialog
import com.charlie.ticklist.ui.CelebrationPopup
import com.charlie.ticklist.ui.RoutePhotoEditor
import com.charlie.ticklist.ui.theme.TicklistClimbingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.charlie.ticklist.settings.AboutScreen
import com.charlie.ticklist.ui.CollectionCoverViewerDialog
import com.charlie.ticklist.ui.CollectionCoverThumbnail






private enum class RouteStatus { FLASH, TOP, ZONE, PROJECT }
private enum class StatusFilter { NONE, FLASH, TOP, ZONE, PROJECT }
private enum class SortColumn { ROUTE, FLASH, TOP, ZONE, PROJECT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsRepository = remember {
                AppSettingsRepository(context)
            }
            val settings by settingsRepository.settings
                .collectAsState(initial = AppSettings())

            TicklistClimbingTheme(
                darkTheme = settings.darkModeEnabled
            ) {
                TicklistApp(
                    settings = settings,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}

@Composable
private fun TicklistApp(
    settings: AppSettings,
    settingsRepository: AppSettingsRepository
) {
    val scope = rememberCoroutineScope()

    var collectionId by remember {
        mutableStateOf<Int?>(null)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    when {
        showAbout -> {
            BackHandler {
                showAbout = false
            }

            AboutScreen(
                onBack = {
                    showAbout = false
                }
            )
        }

        showSettings -> {
            BackHandler {
                showSettings = false
            }

            SettingsScreen(
                settings = settings,
                onBack = {
                    showSettings = false
                },
                onAboutClick = {
                    showAbout = true
                },
                onHapticFeedbackChanged = { enabled ->
                    scope.launch {
                        settingsRepository.setHapticFeedbackEnabled(
                            enabled
                        )
                    }
                },
                onDurationChanged = { durationMs ->
                    scope.launch {
                        settingsRepository
                            .setStatusConfirmationDurationMs(
                                durationMs
                            )
                    }
                },
                onDarkModeChanged = { enabled ->
                    scope.launch {
                        settingsRepository.setDarkModeEnabled(
                            enabled
                        )
                    }
                },
                onCelebrationMessagesChanged = { enabled ->
                    scope.launch {
                        settingsRepository
                            .setCelebrationMessagesEnabled(
                                enabled
                            )
                    }
                }
            )
        }

        collectionId == null -> {
            CollectionsScreen(
                settingsRepository = settingsRepository,
                onOpenCollection = { id ->
                    collectionId = id
                },
                onOpenSettings = {
                    showSettings = true
                }
            )
        }

        else -> {
            BackHandler {
                collectionId = null
            }

            CollectionRoutesScreen(
                collectionId = collectionId!!,
                settings = settings,
                settingsRepository = settingsRepository,
                onBack = {
                    collectionId = null
                },
                onOpenSettings = {
                    showSettings = true
                }
            )
        }
    }
}


@Composable
private fun CollectionsScreen(
    settingsRepository: AppSettingsRepository,
    onOpenCollection: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val db = remember {
        TicklistDatabase.getDatabase(context)
    }

    val collectionDao = db.collectionDao()
    val routeDao = db.routeDao()
    val scope = rememberCoroutineScope()

    val collections by collectionDao
        .observeAllCollections()
        .collectAsState(initial = emptyList())

    val routes by routeDao
        .observeAllRoutes()
        .collectAsState(initial = emptyList())

    var newDialog by remember {
        mutableStateOf(false)
    }

    var editDialog by remember {
        mutableStateOf(false)
    }

    var deleteDialog by remember {
        mutableStateOf(false)
    }

    var editing by remember {
        mutableStateOf<CollectionEntity?>(null)
    }

    var name by remember {
        mutableStateOf("")
    }

    var notes by remember {
        mutableStateOf("")
    }

    var coverPhotoPath by remember {
        mutableStateOf<String?>(null)
    }

    var coverThumbnailPath by remember {
        mutableStateOf<String?>(null)
    }

    var newName by remember {
        mutableStateOf("")
    }

    var newCount by remember {
        mutableStateOf("")
    }

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var collectionViewerPath by remember {
        mutableStateOf<String?>(null)
    }

    val openCoverPhotoPicker = rememberPhotoPicker { uri ->
        val collection = editing

        if (collection != null) {
            scope.launch {
                val oldPhotoPath = coverPhotoPath
                val oldThumbnailPath = coverThumbnailPath

                val newPhotoPath =
                    CollectionPhotoStorage.saveCoverPhoto(
                        context = context,
                        sourceUri = uri,
                        collectionId = collection.id
                    )

                val newThumbnailPath =
                    CollectionPhotoStorage.createCoverThumbnail(
                        originalPath = newPhotoPath
                    )

                coverPhotoPath = newPhotoPath
                coverThumbnailPath = newThumbnailPath

                if (
                    oldPhotoPath != null ||
                    oldThumbnailPath != null
                ) {
                    CollectionPhotoStorage.deleteCoverPhoto(
                        filePath = oldPhotoPath,
                        thumbnailPath = oldThumbnailPath
                    )
                }
            }
        }
    }

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
                        status = null,
                        collectionId = 1
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Meine Sammlungen",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Kurz tippen zum Öffnen, " +
                                "lange drücken zum Bearbeiten",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box {
                    IconButton(
                        onClick = {
                            menuExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menü"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Einstellungen")
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Sammlung importieren")
                            },
                            enabled = false,
                            onClick = {
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newName = ""
                    newCount = ""
                    newDialog = true
                }
            ) {
                Text("+")
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = collections,
                key = { collection ->
                    collection.id
                }
            ) { collection ->

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                onOpenCollection(collection.id)
                            },
                            onLongClick = {
                                editing = collection
                                name = collection.name
                                notes = collection.notes.orEmpty()
                                coverPhotoPath =
                                    collection.coverPhotoPath
                                coverThumbnailPath =
                                    collection.coverThumbnailPath
                                editDialog = true
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CollectionCoverThumbnail(
                            thumbnailPath =
                                collection.coverThumbnailPath,
                            onClick = {
                                onOpenCollection(collection.id)
                            },
                            onLongClick = {
                                collection.coverPhotoPath?.let { path ->
                                    collectionViewerPath = path
                                }
                            }
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${collectionRoutes.size} Routen · " +
                                        "$tops Top ($flashes Flash)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (collectionViewerPath != null) {
        CollectionCoverViewerDialog(
            filePath = collectionViewerPath!!,
            onDismiss = {
                collectionViewerPath = null
            }
        )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                val collectionId =
                                    collectionDao.insertCollection(
                                        CollectionEntity(
                                            name = collectionName,
                                            discipline = "BOULDER",
                                            createdAt =
                                                System.currentTimeMillis()
                                        )
                                    ).toInt()

                                if (count > 0) {
                                    routeDao.insertRoutes(
                                        (1..count).map { number ->
                                            RouteEntity(
                                                number = number,
                                                name = "%02d".format(number),
                                                difficulty = "",
                                                status = null,
                                                collectionId = collectionId
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

                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                        },
                        label = {
                            Text("Notizen / Wettbewerbsdaten")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    CollectionCoverThumbnail(
                        thumbnailPath = coverThumbnailPath,
                        onClick = {
                            if (coverPhotoPath != null) {
                                collectionViewerPath =
                                    coverPhotoPath
                            }
                        },
                        onLongClick = {
                            openCoverPhotoPicker()
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            openCoverPhotoPicker()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (coverPhotoPath == null) {
                                "Sammlungsfoto auswählen"
                            } else {
                                "Sammlungsfoto ersetzen"
                            }
                        )
                    }

                    if (coverPhotoPath != null) {
                        TextButton(
                            onClick = {
                                CollectionPhotoStorage
                                    .deleteCoverPhoto(
                                        filePath = coverPhotoPath,
                                        thumbnailPath =
                                            coverThumbnailPath
                                    )

                                coverPhotoPath = null
                                coverThumbnailPath = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sammlungsfoto löschen")
                        }
                    }

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
                        val collectionName = name.trim()

                        if (
                            collection != null &&
                            collectionName.isNotBlank()
                        ) {
                            scope.launch {
                                collectionDao.updateCollectionDetails(
                                    id = collection.id,
                                    name = collectionName,
                                    notes = notes.ifBlank {
                                        null
                                    },
                                    coverPhotoPath = coverPhotoPath,
                                    coverThumbnailPath =
                                        coverThumbnailPath
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
                    "Die Sammlung und alle zugehörigen Routen " +
                            "werden dauerhaft gelöscht."
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

                                CollectionPhotoStorage
                                    .deleteCoverPhoto(
                                        filePath =
                                            collection.coverPhotoPath,
                                        thumbnailPath =
                                            collection.coverThumbnailPath
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
    settings: AppSettings,
    settingsRepository: AppSettingsRepository,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { TicklistDatabase.getDatabase(context) }
    val routeDao = db.routeDao()
    val collectionDao = db.collectionDao()
    val photoDao = db.routePhotoDao()
    val photoRepository = remember {
        RoutePhotoRepository(context, photoDao)
    }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var celebrationMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(celebrationMessage) {
        if (celebrationMessage != null) {
            delay(3000)
            celebrationMessage = null
        }
    }

    val routes by routeDao
        .observeRoutesForCollection(collectionId)
        .collectAsState(emptyList())

    val mainPhotos by photoDao
        .observeMainPhotosForCollection(collectionId)
        .collectAsState(emptyList())

    val mainPhotoByRouteId = mainPhotos.associateBy {
        it.routeId
    }

    LaunchedEffect(mainPhotos) {
        mainPhotos.forEach { photo ->
            if (!PhotoStorage.fileExists(photo.thumbnailPath)) {
                photoRepository.ensureThumbnail(photo)
            }
        }
    }

    val currentCollection by collectionDao
        .observeCollection(collectionId)
        .collectAsState(initial = null)

    val collectionName = currentCollection?.name ?: ""

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
    var selected by remember {
        mutableStateOf<Set<Int>>(emptySet())
    }
    var filterOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var bulk by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var editingNumber by remember { mutableStateOf<Int?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDifficulty by remember { mutableStateOf("") }
    var editStatus by remember {
        mutableStateOf<RouteStatus?>(null)
    }
    var editStatusAt by remember { mutableStateOf<Long?>(null) }
    var editCompleted by remember { mutableStateOf<Long?>(null) }

    var bulkStatusEnabled by remember { mutableStateOf(false) }
    var bulkStatus by remember {
        mutableStateOf<RouteStatus?>(null)
    }
    var bulkDateEnabled by remember { mutableStateOf(false) }
    var bulkDate by remember { mutableStateOf<Long?>(null) }

    var photoViewerPhoto by remember {
        mutableStateOf<RoutePhotoEntity?>(null)
    }
    var photoViewerPhotos by remember {
        mutableStateOf<List<RoutePhotoEntity>>(emptyList())
    }

    val shown = routes
        .filter { routeFilter(it.status) in filters }
        .sortedWith(routeComparator(sort, ascending))

    val tops = routes.count {
        it.status == "TOP" || it.status == "FLASH"
    }
    val flashes = routes.count { it.status == "FLASH" }
    val zones = routes.count { it.status == "ZONE" }

    fun openRoute(route: RouteEntity) {
        scope.launch {
            val current = routeDao.getRoute(route.number) ?: route
            editingNumber = current.number
            editName = current.name
            editDifficulty = current.difficulty
            editStatus = current.status.toRouteStatus()
            editStatusAt = current.statusChangedAt
            editCompleted = current.completedDate
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
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = collectionName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${shown.size} von ${routes.size} Routen",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        CollectionCoverThumbnail(
                            thumbnailPath = currentCollection?.coverThumbnailPath,
                            onClick = {
                                currentCollection?.coverPhotoPath?.let {
                                    // Viewer wird im nächsten Schritt angeschlossen.
                                }
                            },
                            onLongClick = {
                                // Bearbeitungsdialog wird im nächsten Schritt angeschlossen.
                            }
                        )

                        TextButton(
                            onClick = onBack
                        ) {
                            Text("Sammlungen")
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    menuExpanded = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Menü"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = {
                                    menuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Einstellungen")
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenSettings()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Sammlung exportieren"
                                        )
                                    },
                                    enabled = false,
                                    onClick = {
                                        menuExpanded = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Fortschritt übertragen"
                                        )
                                    },
                                    enabled = false,
                                    onClick = {
                                        menuExpanded = false
                                    }
                                )
                            }
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
                            ) { filters = it }
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
                    editName = ""
                    editDifficulty = ""
                    editStatus = null
                    editStatusAt = null
                    editCompleted = null
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

            items(shown, key = { it.id }) { route ->
                RouteRow(
                    route = route,
                    mainPhoto = mainPhotoByRouteId[route.id],
                    selectionMode = selectionMode,
                    selected = route.number in selected,
                    settings = settings,
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
                        val isSuccessfulStatus =
                            newStatus == RouteStatus.TOP ||
                                    newStatus == RouteStatus.FLASH
                        val isNewManualSuccess =
                            isSuccessfulStatus &&
                                    route.status != newStatus.name

                        scope.launch {
                            routeDao.updateRouteWithDates(
                                number = route.number,
                                name = route.name,
                                difficulty = route.difficulty,
                                status = newStatus.name,
                                statusChangedAt = now,
                                completedDate =
                                    if (isSuccessfulStatus) {
                                        route.completedDate ?: now
                                    } else {
                                        null
                                    },
                                collectionId = collectionId
                            )

                            if (settings.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                            }

                            if (
                                isNewManualSuccess &&
                                settings.celebrationMessagesEnabled
                            ) {
                                val count = settingsRepository.incrementManualSuccessCount()
                                if (count % 1 == 0) {
                                    celebrationMessage = settingsRepository.nextCelebrationMessage()
                                }
                            }
                        }
                    },
                    onEdit = { openRoute(route) },
                    onPhotoClick = { photo ->
                        scope.launch {
                            photoViewerPhotos =
                                photoDao.getPhotosForRoute(
                                    routeId = photo.routeId
                                )
                            photoViewerPhoto = photo
                        }
                    }
                )
            }
        }
    }

    photoViewerPhoto?.let { photo ->
        PhotoViewerDialog(
            selectedPhoto = photo,
            photos = photoViewerPhotos,
            onDismiss = { photoViewerPhoto = null },
            onSelectPhoto = { photoViewerPhoto = it },
            onDeletePhoto = {
                scope.launch {
                    photoRepository.deletePhoto(it)
                    photoViewerPhoto = null
                }
            },
            onSetMainPhoto = {
                scope.launch {
                    photoRepository.setMainPhoto(it)
                    photoViewerPhoto = it.copy(
                        isMainPhoto = true
                    )
                }
            }
        )
    }

    celebrationMessage?.let { message ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            CelebrationPopup(message = message)
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
            name = editName,
            difficulty = editDifficulty,
            status = editStatus,
            statusChangedAt = editStatusAt,
            completedDate = editCompleted,
            onNameChanged = { editName = it },
            onDifficultyChanged = { editDifficulty = it },
            onStatusChanged = { editStatus = it },
            onCompletedDateChanged = { editCompleted = it },
            onDelete = if (editingNumber != null) {
                { delete = true }
            } else {
                null
            },
            onDismiss = {
                dialog = false
                editingNumber = null
            },
            onSave = {
                if (editName.isNotBlank()) {
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val old = routes.firstOrNull {
                            it.number == editingNumber
                        }
                        val savedStatusAt =
                            old?.statusChangedAt
                                ?: editStatusAt ?: now
                        val savedCompleted =
                            if (
                                editStatus == RouteStatus.TOP ||
                                editStatus == RouteStatus.FLASH
                            ) {
                                editCompleted
                            } else {
                                null
                            }

                        if (editingNumber == null) {
                            routeDao.insertRoute(
                                RouteEntity(
                                    number = (
                                            routes.maxOfOrNull {
                                                it.number
                                            }?.plus(1) ?: 1
                                            ),
                                    name = editName,
                                    difficulty = editDifficulty,
                                    status = editStatus?.name,
                                    statusChangedAt = savedStatusAt,
                                    completedDate = savedCompleted,
                                    collectionId = collectionId
                                )
                            )
                        } else {
                            routeDao.updateRouteWithDates(
                                number = editingNumber!!,
                                name = editName,
                                difficulty = editDifficulty,
                                status = editStatus?.name,
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
            onStatusChanged = { bulkStatus = it },
            onDateEnabledChanged = { bulkDateEnabled = it },
            onDateChanged = { bulkDate = it },
            onDismiss = { bulk = false },
            onSave = {
                scope.launch {
                    val now = System.currentTimeMillis()
                    routes
                        .filter { it.number in selected }
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
                                statusChangedAt =
                                    if (bulkStatusEnabled) {
                                        now
                                    } else {
                                        route.statusChangedAt
                                    },
                                completedDate =
                                    if (bulkDateEnabled) {
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
            onDismissRequest = { delete = false },
            title = { Text("Route löschen?") },
            text = {
                Text("Diese Route wird dauerhaft gelöscht.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingNumber?.let { number ->
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
                    onClick = { delete = false }
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
    DropdownMenu(expanded, onDismiss) {
        FilterItem("Flash", StatusFilter.FLASH in selectedFilters) {
            onChanged(selectedFilters.toggleFilter(StatusFilter.FLASH))
        }
        FilterItem("Top", StatusFilter.TOP in selectedFilters) {
            onChanged(selectedFilters.toggleFilter(StatusFilter.TOP))
        }
        FilterItem("Zone", StatusFilter.ZONE in selectedFilters) {
            onChanged(selectedFilters.toggleFilter(StatusFilter.ZONE))
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
            onChanged(selectedFilters.toggleFilter(StatusFilter.NONE))
        }
        DropdownMenuItem(
            text = { Text("Alle") },
            onClick = { onChanged(allFilters) }
        )
        DropdownMenuItem(
            text = { Text("Keine") },
            onClick = { onChanged(emptySet()) }
        )
        DropdownMenuItem(
            text = { Text("Abgeschlossene aus") },
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
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = { onClick() }
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
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text("Name oder Nummer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = difficulty,
                    onValueChange = onDifficultyChanged,
                    label = { Text("Schwierigkeit") },
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
                        "Speichere die Route zuerst, " +
                                "um Fotos hinzuzufügen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    "Status",
                    style = MaterialTheme.typography.titleMedium
                )

                StatusChoice("Kein Status", status == null) {
                    onStatusChanged(null)
                }
                StatusChoice(
                    "Flash",
                    status == RouteStatus.FLASH
                ) {
                    onStatusChanged(RouteStatus.FLASH)
                }
                StatusChoice(
                    "Top",
                    status == RouteStatus.TOP
                ) {
                    onStatusChanged(RouteStatus.TOP)
                }
                StatusChoice(
                    "Zone",
                    status == RouteStatus.ZONE
                ) {
                    onStatusChanged(RouteStatus.ZONE)
                }
                StatusChoice(
                    "Projekt",
                    status == RouteStatus.PROJECT
                ) {
                    onStatusChanged(RouteStatus.PROJECT)
                }

                if (statusChangedAt != null) {
                    Text(
                        "Status eingetragen: ${
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
                        "Erfolgsdatum: ${
                            completedDate?.let {
                                formatDate(it)
                            } ?: "nicht eingetragen"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context,
                                completedDate,
                                onCompletedDateChanged
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
            Button(onClick = onSave) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
        title = { Text("$selectedCount Routen bearbeiten") },
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
                    StatusChoice(
                        "Flash",
                        status == RouteStatus.FLASH
                    ) { onStatusChanged(RouteStatus.FLASH) }
                    StatusChoice(
                        "Top",
                        status == RouteStatus.TOP
                    ) { onStatusChanged(RouteStatus.TOP) }
                    StatusChoice(
                        "Zone",
                        status == RouteStatus.ZONE
                    ) { onStatusChanged(RouteStatus.ZONE) }
                    StatusChoice(
                        "Projekt",
                        status == RouteStatus.PROJECT
                    ) { onStatusChanged(RouteStatus.PROJECT) }
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
                        "Datum: ${
                            date?.let {
                                formatDate(it)
                            } ?: "nicht gesetzt"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            showDatePicker(
                                context,
                                date,
                                onDateChanged
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Datum auswählen")
                    }
                    if (date != null) {
                        TextButton(
                            onClick = { onDateChanged(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Datum löschen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Auf $selectedCount anwenden")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
        Button(onClick, Modifier.fillMaxWidth()) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick, Modifier.fillMaxWidth()) {
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
        modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Text(
            "Top: $topCount ($flashCount Flash) · Zone: $zoneCount",
            Modifier.padding(10.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onSort(SortColumn.ROUTE) },
            modifier = Modifier.width(86.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "Route" + if (column == SortColumn.ROUTE) {
                    if (ascending) " ↑" else " ↓"
                } else {
                    ""
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        listOf(
            "Flash" to SortColumn.FLASH,
            "Top" to SortColumn.TOP,
            "Zone" to SortColumn.ZONE,
            "Projekt" to SortColumn.PROJECT
        ).forEach { (label, sortColumn) ->
            TextButton(
                onClick = { onSort(sortColumn) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    label + if (column == sortColumn) {
                        if (ascending) " ↑" else " ↓"
                    } else {
                        ""
                    },
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
    settings: AppSettings,
    onSelectedChange: () -> Unit,
    onStatusChange: (RouteStatus) -> Unit,
    onEdit: () -> Unit,
    onPhotoClick: (RoutePhotoEntity) -> Unit
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
        Modifier
            .fillMaxWidth()
            .background(rowBackground)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
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
                        Modifier
                            .width(28.dp)
                            .padding(end = 2.dp)
                            .pointerInput(
                                route.id,
                                selectionMode
                            ) {
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
                            route.name,
                            style =
                                MaterialTheme.typography
                                    .titleSmall,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                        if (route.difficulty.isNotBlank()) {
                            Text(
                                route.difficulty,
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (mainPhoto != null) {
                        RouteRowThumbnail(
                            photo = mainPhoto,
                            onClick = {
                                onPhotoClick(mainPhoto)
                            },
                            onLongClick = { onEdit() }
                        )
                    } else {
                        RoutePhotoPlaceholder(
                            onClick = {},
                            onLongClick = { onEdit() }
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    listOf(
                        "Flash" to RouteStatus.FLASH,
                        "Top" to RouteStatus.TOP,
                        "Zone" to RouteStatus.ZONE,
                        "Projekt" to RouteStatus.PROJECT
                    ).forEach { (label, routeStatus) ->
                        StatusButton(
                            label = label,
                            selected =
                                route.status == routeStatus.name,
                            modifier = Modifier.weight(1f),
                            durationMs =
                                settings
                                    .statusConfirmationDurationMs,
                            onProgress = {
                                rowProgress = it
                            },
                            onComplete = {
                                rowProgress = 0f
                                onStatusChange(routeStatus)
                            }
                        )
                    }
                }

                if (rowProgress > 0f) {
                    BorderProgress(
                        rowProgress,
                        MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteRowThumbnail(
    photo: RoutePhotoEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val previewPath = photo.thumbnailPath ?: photo.filePath
    val imageBitmap = remember(previewPath) {
        val file = java.io.File(previewPath)
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
        Modifier
            .size(48.dp)
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
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
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "–",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RoutePhotoPlaceholder(
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            }
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "+",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatusButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    durationMs: Int = 1500,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    Box(
        modifier
            .height(44.dp)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Button(
                {},
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
            ) {
                Text(label, fontSize = 10.sp, maxLines = 1)
            }
        } else {
            OutlinedButton(
                {},
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp)
            ) {
                Text(label, fontSize = 10.sp, maxLines = 1)
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
                                progress.snapTo(0f)
                                onProgress(0f)

                                val job = launch {
                                    progress.animateTo(
                                        1f,
                                        tween(durationMs)
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
                                    progress.snapTo(0f)
                                    onProgress(0f)
                                }
                                progress.snapTo(0f)
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
): StatusFilter? = when (status) {
    null -> StatusFilter.NONE
    "FLASH" -> StatusFilter.FLASH
    "TOP" -> StatusFilter.TOP
    "ZONE" -> StatusFilter.ZONE
    "PROJECT" -> StatusFilter.PROJECT
    else -> null
}

private fun routeComparator(
    column: SortColumn,
    ascending: Boolean
): Comparator<RouteEntity> {
    val comp = when (column) {
        SortColumn.ROUTE -> compareBy<RouteEntity> {
            it.number
        }
        SortColumn.FLASH -> compareByDescending<RouteEntity> {
            it.status == "FLASH"
        }.thenBy { it.number }
        SortColumn.TOP -> compareByDescending<RouteEntity> {
            it.status == "TOP"
        }.thenBy { it.number }
        SortColumn.ZONE -> compareByDescending<RouteEntity> {
            it.status == "ZONE"
        }.thenBy { it.number }
        SortColumn.PROJECT -> compareByDescending<RouteEntity> {
            it.status == "PROJECT"
        }.thenBy { it.number }
    }
    return if (ascending) comp else comp.reversed()
}

private fun Set<StatusFilter>.toggleFilter(
    filter: StatusFilter
): Set<StatusFilter> =
    if (filter in this) this - filter else this + filter

private fun String?.toRouteStatus(): RouteStatus? =
    this?.let {
        runCatching { RouteStatus.valueOf(it) }.getOrNull()
    }

private fun formatDate(t: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        .format(Date(t))

private fun formatDateTime(t: Long): String =
    SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        .format(Date(t))

private fun showDatePicker(
    context: Context,
    currentDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val c = Calendar.getInstance()
    if (currentDate != null) c.timeInMillis = currentDate
    DatePickerDialog(
        context,
        { _, y, m, d ->
            onDateSelected(
                Calendar.getInstance().apply {
                    set(y, m, d, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            )
        },
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH),
        c.get(Calendar.DAY_OF_MONTH)
    ).show()
}
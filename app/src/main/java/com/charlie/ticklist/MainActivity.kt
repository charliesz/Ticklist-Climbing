package com.charlie.ticklist

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charlie.ticklist.ui.theme.TicklistClimbingTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class RouteStatus {
    FLASH,
    TOP,
    ZONE,
    PROJECT
}

data class ClimbingRoute(
    val number: Int,
    val name: String,
    val difficulty: String,
    val status: RouteStatus? = null
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TicklistClimbingTheme {
                TicklistHomeScreen()
            }
        }
    }
}

@Composable
fun TicklistHomeScreen() {
    val routes = remember {
        mutableStateListOf(
            *List(30) { index ->
                ClimbingRoute(
                    number = index + 1,
                    name = "Route ${String.format("%02d", index + 1)}",
                    difficulty = "",
                    status = null
                )
            }.toTypedArray()
        )
    }

    var showRouteDialog by remember { mutableStateOf(false) }
    var editingRouteIndex by remember { mutableStateOf<Int?>(null) }
    var routeName by remember { mutableStateOf("") }
    var routeDifficulty by remember { mutableStateOf("") }
    var routeStatus by remember { mutableStateOf<RouteStatus?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Ticklist Climbing",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "${routes.size} Routen",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Status 2 Sekunden gedrückt halten",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRouteIndex = null
                    routeName = ""
                    routeDifficulty = ""
                    routeStatus = null
                    showRouteDialog = true
                }
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RouteTableHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(routes) { index, route ->
                    RouteRow(
                        route = route,
                        onStatusChange = { newStatus ->
                            routes[index] = route.copy(status = newStatus)
                        },
                        onEdit = {
                            editingRouteIndex = index
                            routeName = route.name
                            routeDifficulty = route.difficulty
                            routeStatus = route.status
                            showRouteDialog = true
                        },
                        onDelete = {
                            routes.removeAt(index)
                        }
                    )
                }
            }
        }
    }

    if (showRouteDialog) {
        AlertDialog(
            onDismissRequest = {
                showRouteDialog = false
                editingRouteIndex = null
            },
            title = {
                Text(
                    text = if (editingRouteIndex == null) {
                        "Route hinzufügen"
                    } else {
                        "Route bearbeiten"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = routeName,
                        onValueChange = { routeName = it },
                        label = { Text("Name oder Nummer") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = routeDifficulty,
                        onValueChange = { routeDifficulty = it },
                        label = { Text("Schwierigkeit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Status manuell festlegen",
                        style = MaterialTheme.typography.titleMedium
                    )

                    StatusSelectionButton(
                        text = "Kein Status",
                        selected = routeStatus == null
                    ) {
                        routeStatus = null
                    }

                    StatusSelectionButton(
                        text = "Flash",
                        selected = routeStatus == RouteStatus.FLASH
                    ) {
                        routeStatus = RouteStatus.FLASH
                    }

                    StatusSelectionButton(
                        text = "Top",
                        selected = routeStatus == RouteStatus.TOP
                    ) {
                        routeStatus = RouteStatus.TOP
                    }

                    StatusSelectionButton(
                        text = "Zone",
                        selected = routeStatus == RouteStatus.ZONE
                    ) {
                        routeStatus = RouteStatus.ZONE
                    }

                    StatusSelectionButton(
                        text = "Projekt",
                        selected = routeStatus == RouteStatus.PROJECT
                    ) {
                        routeStatus = RouteStatus.PROJECT
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeName.isNotBlank()) {
                            val index = editingRouteIndex

                            if (index == null) {
                                val nextNumber =
                                    if (routes.isEmpty()) {
                                        1
                                    } else {
                                        routes.maxOf { it.number } + 1
                                    }

                                routes.add(
                                    ClimbingRoute(
                                        number = nextNumber,
                                        name = routeName,
                                        difficulty = routeDifficulty,
                                        status = routeStatus
                                    )
                                )
                            } else {
                                routes[index] = routes[index].copy(
                                    name = routeName,
                                    difficulty = routeDifficulty,
                                    status = routeStatus
                                )
                            }

                            showRouteDialog = false
                            editingRouteIndex = null
                        }
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRouteDialog = false
                        editingRouteIndex = null
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun RouteTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Route",
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.labelLarge
        )

        HeaderText("Flash")
        HeaderText("Top")
        HeaderText("Zone")
        HeaderText("Projekt")
        HeaderText("Optionen")
    }
}

@Composable
fun HeaderText(text: String) {
    Text(
        text = text,
        modifier = Modifier.width(68.dp),
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun RouteRow(
    route: ClimbingRoute,
    onStatusChange: (RouteStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(120.dp)
            ) {
                Text(
                    text = "%02d – %s".format(route.number, route.name),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (route.difficulty.isBlank()) {
                        "Keine Schwierigkeit"
                    } else {
                        route.difficulty
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            StatusButton(
                label = "Flash",
                selected = route.status == RouteStatus.FLASH
            ) {
                onStatusChange(RouteStatus.FLASH)
            }

            StatusButton(
                label = "Top",
                selected = route.status == RouteStatus.TOP
            ) {
                onStatusChange(RouteStatus.TOP)
            }

            StatusButton(
                label = "Zone",
                selected = route.status == RouteStatus.ZONE
            ) {
                onStatusChange(RouteStatus.ZONE)
            }

            StatusButton(
                label = "Projekt",
                selected = route.status == RouteStatus.PROJECT
            ) {
                onStatusChange(RouteStatus.PROJECT)
            }

            Column(
                modifier = Modifier.width(110.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = onEdit) {
                    Text("Editieren")
                }

                TextButton(onClick = onDelete) {
                    Text("Löschen")
                }
            }
        }
    }
}

@Composable
fun StatusSelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val progress = remember {
        Animatable(0f)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .width(68.dp)
            .height(48.dp)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 0.dp
                ),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        } else {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(label) {
                    detectTapGestures(
                        onPress = {
                            coroutineScope {
                                val animationJob = launch {
                                    progress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 2000
                                        )
                                    )
                                }

                                val releasedSuccessfully =
                                    tryAwaitRelease()

                                animationJob.cancel()

                                if (
                                    releasedSuccessfully &&
                                    progress.value >= 0.999f
                                ) {
                                    onClick()
                                }

                                progress.snapTo(0f)
                            }
                        }
                    )
                }
        )

        if (progress.value > 0f) {
            BorderProgress(
                progress = progress.value,
                color = primaryColor
            )
        }
    }
}

@Composable
fun BorderProgress(
    progress: Float,
    color: Color
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = 3.dp.toPx()
        val cornerRadius = 12.dp.toPx()

        val borderPath = AndroidPath()

        borderPath.addRoundRect(
            RectF(
                strokeWidth / 2f,
                strokeWidth / 2f,
                size.width - strokeWidth / 2f,
                size.height - strokeWidth / 2f
            ),
            cornerRadius,
            cornerRadius,
            AndroidPath.Direction.CW
        )

        val pathMeasure = PathMeasure(borderPath, false)
        val progressPath = AndroidPath()

        pathMeasure.getSegment(
            0f,
            pathMeasure.length * progress.coerceIn(0f, 1f),
            progressPath,
            true
        )

        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = color.toArgb()

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(
                progressPath,
                paint
            )
        }
    }
}

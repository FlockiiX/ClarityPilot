package com.claritypilot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.json.Json
import java.io.InputStream


import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.* // Viele neue Imports hier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest


class MainActivity : ComponentActivity() {

    // Dein Permission Launcher (unverändert übernommen)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Hier könntest du auf die Entscheidung reagieren (optional)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission Logik (unverändert übernommen, nur in onCreate platziert)
        // Hinweis: POST_NOTIFICATIONS gibt es erst ab Android 13 (API 33).
        // Ein Check auf die Version ist gute Praxis, um Abstürze auf alten Geräten zu vermeiden,
        // falls dein compileSdk < 33 ist.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Hier startet die Compose UI mit den zwei Reitern
        // (Ersetzt setContentView(R.layout.widget_initial_layout))
        setContent {
            MaterialTheme {
                MainTabScreen()
            }
        }
    }
}

@Composable
fun ActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp), // Etwas Abstand
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Abstand zwischen Icon und Text
    ) {
        // --- Icon ---
        // Wir verwenden Coil, um das Bild von einer URL zu laden
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(item.icon ?: "https://cdn-icons-png.flaticon.com/512/25/25231.png") // Fallback-Icon (GitHub)
                .crossfade(true)
                .build(),
            contentDescription = item.type,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape) // Macht das Icon rund
        )

        // --- Beschreibung ---
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f) // Nimmt den verfügbaren Platz ein
        )

        // --- Dauer ---
        Text(
            text = item.duration,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Etwas dezenter
        )
    }
}

// Datenklasse für die Tab-Konfiguration
data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit
)

@Composable
fun MainTabScreen() {
    val tabs = listOf(
        TabItem("View", Icons.Filled.Home) { ViewScreen() },
        TabItem("Settings", Icons.Filled.Settings) { SettingsScreen() }
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Passen Sie den Scaffold an:
    Scaffold(
        // Die TabRow wird jetzt hier in der bottomBar platziert
        bottomBar = {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) }
                    )
                }
            }
        }
        // Der topBar-Slot bleibt leer
    ) { innerPadding ->
        // Der wischbare Bereich (HorizontalPager)
        // WICHTIG: Das innerPadding vom Scaffold wird jetzt automatisch den
        // Platz für die untere Leiste freihalten.
        HorizontalPager(
            // Hinweis: Der Column-Wrapper ist hier nicht unbedingt nötig.
            // Das padding kann direkt am Pager angewendet werden.
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = pagerState
        ) { pageIndex ->
            tabs[pageIndex].screen()
        }
    }
}

// --- Beispiel Inhalt für den "View" Screen ---

// DURCH DIESE NEUE VERSION:
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewScreen() {
    val context = LocalContext.current

    val timelineData = produceState<TimelineData>(initialValue = emptyMap(), producer = {
        val jsonString = loadJsonFromAssets(context, "timeline.json")
        val json = Json { ignoreUnknownKeys = true }
        value = if (jsonString.isNotBlank()) json.decodeFromString(jsonString) else emptyMap()
    }).value

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (timelineData.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lade Daten oder keine Einträge gefunden...")
                }
            }
        } else {
            timelineData.forEach { (day, activities) ->
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(activities) { activity ->
                    ActivityRow(item = activity)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

// DIESE FUNKTION HINZUFÜGEN:
fun loadJsonFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        "{}" // Leeres JSON bei Fehler zurückgeben
    }
}

// --- Beispiel Inhalt für den "Settings" Screen ---
@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Hier sind die Einstellungen", style = MaterialTheme.typography.headlineMedium)
    }
}
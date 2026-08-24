package com.lumenconnection.stream.ui

import android.widget.Toast
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lumenconnection.stream.R
import com.lumenconnection.stream.download.DownloadService
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val MUSIC = "music"
    const val VIDEO = "video"
    const val QUEUE = "queue"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val PLAYER = "player/{mediaId}"
    fun player(mediaId: Long) = "player/$mediaId"
}

/**
 * Ordem canônica das abas, espelhando o TAB_ORDER do desktop (app.rs). As abas
 * exclusivas de desktop (Converter, Folders, Games, Cloud, Achievements) ficam
 * de fora; o resto mantém ícone, ordem e rótulo.
 */
private data class NavEntry(val icon: String, val labelRes: Int, val route: String)

private val NAV_ENTRIES = listOf(
    NavEntry("🏠", R.string.nav_home, Routes.HOME),
    NavEntry("🎵", R.string.nav_music, Routes.MUSIC),
    NavEntry("🎬", R.string.nav_video, Routes.VIDEO),
    NavEntry("📋", R.string.nav_queue, Routes.QUEUE),
    NavEntry("📁", R.string.nav_library, Routes.LIBRARY),
    NavEntry("⚙", R.string.nav_settings, Routes.SETTINGS),
    NavEntry("❓", R.string.nav_help, Routes.HELP),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(sharedUrl: String?, onSharedUrlConsumed: () -> Unit) {
    val c = Lumen.colors
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val shareScope = rememberCoroutineScope()
    val context = LocalContext.current

    // O diálogo de formato do link compartilhado vive aqui, e não na Home: se
    // dependesse de navegar até ela, a recriação da tela durante a navegação
    // descartava o estado do diálogo e o compartilhamento sumia.
    var pendingShareUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            pendingShareUrl = sharedUrl
            onSharedUrlConsumed()
        }
    }
    pendingShareUrl?.let { target ->
        FormatDialog(
            onDismiss = { pendingShareUrl = null },
            onSelect = { format ->
                pendingShareUrl = null
                shareScope.launch {
                    DownloadService.enqueue(context, target, format)
                    Toast.makeText(context, R.string.share_added, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    val isPlayer = currentRoute?.startsWith("player/") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isPlayer,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = c.bgSidebar,
                drawerContentColor = c.text,
            ) {
                SidebarContent(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = c.bgApp,
            topBar = {
                if (!isPlayer) {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.app_name),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.text,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Text("☰", fontSize = 20.sp, color = c.text)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = c.bgSidebar,
                            titleContentColor = c.text,
                        ),
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.bgApp)
                    .padding(padding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(onOpenQueue = { navController.navigate(Routes.QUEUE) })
                }
                composable(Routes.MUSIC) { MediaTabScreen(audio = true) }
                composable(Routes.VIDEO) { MediaTabScreen(audio = false) }
                composable(Routes.QUEUE) { QueueScreen() }
                composable(Routes.LIBRARY) {
                    LibraryScreen(onOpenMedia = { id -> navController.navigate(Routes.player(id)) })
                }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.HELP) { HelpScreen() }
                composable(Routes.PLAYER) { entry ->
                    val id = entry.arguments?.getString("mediaId")?.toLongOrNull()
                        ?: return@composable
                    PlayerScreen(mediaId = id, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

/** Barra lateral do desktop: marca no topo, navegação e rodapé de armazenamento. */
@Composable
private fun SidebarContent(currentRoute: String?, onNavigate: (String) -> Unit) {
    val c = Lumen.colors
    Column(
        Modifier
            .fillMaxHeight()
            .background(c.bgSidebar)
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo_lumen_stream),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    stringResource(R.string.app_name),
                    color = c.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("Lumen Connection", color = c.textFaint, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(22.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            NAV_ENTRIES.forEach { entry ->
                NavItem(
                    icon = entry.icon,
                    label = stringResource(entry.labelRes),
                    selected = currentRoute == entry.route,
                    onClick = { onNavigate(entry.route) },
                )
            }
        }

        StorageFooter()
        Spacer(Modifier.height(12.dp))
    }
}

/** Equivalente ao storage_footer() do dashboard.rs, com o anel de uso do disco. */
@Composable
private fun StorageFooter() {
    val c = Lumen.colors
    val stats = remember {
        runCatching {
            val fs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            val total = fs.blockCountLong * fs.blockSizeLong
            val free = fs.availableBlocksLong * fs.blockSizeLong
            total to free
        }.getOrNull()
    }
    val total = stats?.first ?: 0L
    val free = stats?.second ?: 0L
    val usedFrac = if (total > 0) ((total - free).toFloat() / total).coerceIn(0f, 1f) else 0f

    LumenCard(contentPadding = 12) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.storage_all), color = c.text, fontSize = 13.sp)
                Text(
                    stringResource(R.string.storage_free, formatSize(free)),
                    color = c.textMuted,
                    fontSize = 11.5.sp,
                )
            }
            Box(contentAlignment = Alignment.Center) {
                val track = c.bgCardHover
                val accent = c.accent
                Box(
                    Modifier
                        .size(34.dp)
                        .drawBehind {
                            val stroke = 3.dp.toPx()
                            val inset = stroke / 2
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            drawArc(
                                color = track,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke),
                            )
                            drawArc(
                                color = accent,
                                startAngle = -90f,
                                sweepAngle = 360f * usedFrac,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke),
                            )
                        },
                )
                Text("${(usedFrac * 100).toInt()}%", color = c.text, fontSize = 10.sp)
            }
        }
    }
}

/** format_size() do desktop. */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "${value.toInt()} ${units[unit]}"
    else String.format("%.1f %s", value, units[unit])
}


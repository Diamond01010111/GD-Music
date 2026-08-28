package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.diamond.gdapplication.AudioQuality
import com.diamond.gdapplication.RequestTracker
import com.diamond.gdapplication.ui.components.AutoSizeSingleLineText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    defaultBitrate: Int,
    darkMode: Boolean,
    onDefaultBitrateChange: (Int) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenSearch: () -> Unit,
    onSongClick: (String) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var requestCount by remember { mutableIntStateOf(RequestTracker.countLastFiveMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            requestCount = RequestTracker.countLastFiveMinutes()
            delay(1_000L)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                requestCount = requestCount,
                defaultBitrate = defaultBitrate,
                darkMode = darkMode,
                onDefaultBitrateChange = onDefaultBitrateChange,
                onDarkModeChange = onDarkModeChange
            )
        }
    ) {
        HomeContent(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onOpenSearch = onOpenSearch,
            onSongClick = onSongClick
        )
    }
}

@Composable
private fun HomeContent(
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onSongClick: (String) -> Unit
) {
    val demoSongs = listOf(
        "晴天" to "周杰伦",
        "夜曲" to "周杰伦",
        "稻香" to "周杰伦",
        "七里香" to "周杰伦"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onOpenDrawer, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "打开侧边栏")
            }
            Card(
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                onClick = onOpenSearch
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    AutoSizeSingleLineText(
                        text = "搜索歌曲、歌手、专辑或网易云歌单",
                        style = MaterialTheme.typography.bodyLarge,
                        minFontSize = 11.sp,
                        maxFontSize = 16.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                }
            }
        }

        Text(
            "推荐歌曲",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(demoSongs) { song ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSongClick("${song.first} ${song.second}") }
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(song.first, style = MaterialTheme.typography.titleMedium)
                            Text(song.second, style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "播放 ${song.first}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawer(
    requestCount: Int,
    defaultBitrate: Int,
    darkMode: Boolean,
    onDefaultBitrateChange: (Int) -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    var showQualityChoices by remember { mutableStateOf(false) }
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight()) {
        Text(
            "GD Music",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(20.dp)
        )
        ListItem(
            headlineContent = { Text("最近 5 分钟 GD 音乐台请求") },
            supportingContent = { Text("$requestCount 次") }
        )
        HorizontalDivider()
        Text(
            "设置",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
        )
        NavigationDrawerItem(
            label = { Text("默认音质：${AudioQuality.fromBitrate(defaultBitrate).label}") },
            selected = false,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = { showQualityChoices = !showQualityChoices },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (showQualityChoices) {
            AudioQuality.entries.forEach { quality ->
                NavigationDrawerItem(
                    label = { Text(quality.label) },
                    selected = quality.bitrate == defaultBitrate,
                    icon = if (quality.bitrate == defaultBitrate) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onDefaultBitrateChange(quality.bitrate)
                        showQualityChoices = false
                    },
                    modifier = Modifier.padding(start = 32.dp, end = 12.dp)
                )
            }
        }
        ListItem(
            headlineContent = { Text("深色模式") },
            trailingContent = {
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
            },
            modifier = Modifier.fillMaxWidth().clickable { onDarkModeChange(!darkMode) }
        )
        Spacer(Modifier.weight(1f))
    }
}

package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diamond.gdapplication.Track
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.ui.components.SearchControls
import com.diamond.gdapplication.ui.components.TrackMoreBottomSheet

@Composable
fun SearchResultsScreen(
    keyword: String,
    category: SearchCategory,
    source: String,
    tracks: List<Track>,
    isSearching: Boolean,
    onBack: () -> Unit,
    onSearch: (String, SearchCategory, String) -> Unit,
    onTrackClick: (Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onPlayNext: (Track) -> Unit
) {
    var query by remember { mutableStateOf(keyword) }
    var selectedCategory by remember { mutableStateOf(category) }
    var selectedSource by remember { mutableStateOf(source) }
    var moreTrack by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(keyword, category, source) {
        query = keyword
        selectedCategory = category
        selectedSource = source
    }

    fun search(
        searchKeyword: String = query,
        searchCategory: SearchCategory = selectedCategory,
        searchSource: String = selectedSource
    ) {
        val clean = searchKeyword.trim()
        if (clean.isNotEmpty() && !isSearching) {
            query = clean
            selectedCategory = searchCategory
            selectedSource = searchSource
            onSearch(clean, searchCategory, searchSource)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回搜索")
            }
            Text(
                "搜索结果",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }

        SearchControls(
            keyword = query,
            category = selectedCategory,
            sourceValue = selectedSource,
            isSearching = isSearching,
            onKeywordChange = { query = it },
            onCategoryChange = { newCategory ->
                val nextSource = if (
                    newCategory == SearchCategory.NETEASE_PLAYLIST
                ) {
                    "netease"
                } else {
                    selectedSource
                }
                search(query, newCategory, nextSource)
            },
            onSourceChange = { newSource ->
                val nextCategory = if (
                    selectedCategory == SearchCategory.NETEASE_PLAYLIST &&
                    newSource != "netease"
                ) {
                    SearchCategory.SONG
                } else {
                    selectedCategory
                }
                search(query, nextCategory, newSource)
            },
            onSubmit = { search() }
        )

        if (isSearching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        if (tracks.isEmpty() && !isSearching) {
            Text(
                "没有找到搜索结果",
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = tracks.size,
                    key = { index ->
                        "${tracks[index].source}-${tracks[index].id}-$index"
                    }
                ) { index ->
                    val track = tracks[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onTrackClick(index) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                                Text(
                                    track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                                if (track.album.isNotBlank()) {
                                    Text(
                                        track.album,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = { moreTrack = track }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                        }
                    }
                }
            }
        }
    }

    moreTrack?.let { track ->
        TrackMoreBottomSheet(
            track = track,
            onDismiss = { moreTrack = null },
            onPlayNext = onPlayNext,
            onAddToPlaylist = onAddToPlaylist,
            onFavorite = onFavorite,
            onSearchArtist = { artist ->
                search(artist, SearchCategory.SONG, selectedSource)
            },
            onSearchAlbum = { album ->
                search(album, SearchCategory.ALBUM, selectedSource)
            }
        )
    }
}

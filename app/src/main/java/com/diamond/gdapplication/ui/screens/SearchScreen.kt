package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.diamond.gdapplication.data.SearchHistoryStore
import com.diamond.gdapplication.model.MusicSource
import com.diamond.gdapplication.model.SearchCategory

@Composable
fun SearchScreen(
    isSearching: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSearch: (
        keyword: String,
        category: SearchCategory,
        source: String
    ) -> Unit
) {
    val context = LocalContext.current

    val historyStore = remember {
        SearchHistoryStore(context.applicationContext)
    }

    val sources = remember {
        listOf(
            // 当前推荐稳定源
            MusicSource("网易云", "netease", true),
            MusicSource("JOOX", "joox", true),
            MusicSource("哔哩哔哩", "bilibili", true),

            // 其他支持但可能暂不可用的源
            MusicSource("腾讯音乐", "tencent", false),
            MusicSource("酷我", "kuwo", false),
            MusicSource("Tidal", "tidal", false),
            MusicSource("Qobuz", "qobuz", false),
            MusicSource("Apple Music", "apple", false),
            MusicSource("YouTube Music", "ytmusic", false),
            MusicSource("Spotify", "spotify", false)
        )
    }

    var keyword by rememberSaveable {
        mutableStateOf("")
    }

    var selectedCategory by rememberSaveable {
        mutableStateOf(SearchCategory.SONG)
    }

    var selectedSourceValue by rememberSaveable {
        mutableStateOf("netease")
    }

    val selectedSource = sources.firstOrNull {
        it.value == selectedSourceValue
    } ?: sources.first()

    var sourceMenuExpanded by remember {
        mutableStateOf(false)
    }

    var history by remember {
        mutableStateOf(historyStore.load())
    }

    fun submitSearch(searchKeyword: String) {
        val cleanKeyword = searchKeyword.trim()

        if (cleanKeyword.isEmpty() || isSearching) {
            return
        }

        historyStore.add(cleanKeyword)
        history = historyStore.load()

        onSearch(
            cleanKeyword,
            selectedCategory,
            selectedSourceValue
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }

            Text(
                text = "搜索",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 10.dp
                )
            )
        }

        OutlinedTextField(
            value = keyword,
            onValueChange = {
                keyword = it
            },
            enabled = !isSearching,
            label = {
                Text("输入关键词")
            },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    enabled = keyword.isNotBlank() && !isSearching,
                    onClick = {
                        submitSearch(keyword)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    submitSearch(keyword)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = category

                        if (category == SearchCategory.NETEASE_PLAYLIST) {
                            selectedSourceValue = "netease"
                        }
                    },
                    label = {
                        Text(category.label)
                    }
                )
            }

            item {
                Box {
                    FilterChip(
                        selected = false,
                        onClick = {
                            sourceMenuExpanded = true
                        },
                        label = {
                            Text("源：${selectedSource.label}")
                        }
                    )

                    DropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = {
                            sourceMenuExpanded = false
                        }
                    ) {
                        sources.forEach { source ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(source.label)

                                        if (source.recommended) {
                                            Text(
                                                text = "推荐稳定源",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedSourceValue = source.value
                                    sourceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isSearching) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(
                text = "最近搜索",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        historyStore.clear()
                        history = emptyList()
                    }
                ) {
                    Text("清除")
                }
            }
        }

        if (history.isEmpty()) {
            Text(
                text = "暂无搜索记录",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(history) { historyKeyword ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            keyword = historyKeyword
                            submitSearch(historyKeyword)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null
                            )

                            Text(
                                text = historyKeyword,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
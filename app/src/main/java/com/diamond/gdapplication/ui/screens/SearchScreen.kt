package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.diamond.gdapplication.data.SearchHistoryStore
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.ui.components.SearchControls

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
    var keyword by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(SearchCategory.SONG) }
    var source by rememberSaveable { mutableStateOf("netease") }
    var history by remember { mutableStateOf(historyStore.load()) }

    fun submit(value: String = keyword) {
        val clean = value.trim()
        if (clean.isEmpty() || isSearching) return

        keyword = clean
        historyStore.add(clean)
        history = historyStore.load()
        onSearch(clean, category, source)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text(
                "搜索",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }

        SearchControls(
            keyword = keyword,
            category = category,
            sourceValue = source,
            isSearching = isSearching,
            onKeywordChange = { keyword = it },
            onCategoryChange = {
                category = it
                if (it == SearchCategory.NETEASE_PLAYLIST) {
                    source = "netease"
                }
            },
            onSourceChange = { source = it },
            onSubmit = { submit() }
        )

        if (isSearching) {
            CircularProgressIndicator(Modifier.padding(top = 20.dp))
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            Text(
                "最近搜索",
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
            Text("暂无搜索记录", modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(history) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { submit(item) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp)) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Text(item, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

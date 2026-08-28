package com.diamond.gdapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diamond.gdapplication.model.SearchCategory
import com.diamond.gdapplication.model.supportedMusicSources

@Composable
fun SearchControls(
    keyword: String,
    category: SearchCategory,
    sourceValue: String,
    isSearching: Boolean,
    onKeywordChange: (String) -> Unit,
    onCategoryChange: (SearchCategory) -> Unit,
    onSourceChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var showSources by remember { mutableStateOf(false) }
    val source = supportedMusicSources.firstOrNull {
        it.value == sourceValue
    } ?: supportedMusicSources.first()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = { showSources = true },
            modifier = Modifier.size(52.dp),
            shape = CircleShape
        ) {
            Text(
                text = source.label.take(1),
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            enabled = !isSearching,
            placeholder = {
                Text(
                    when (category) {
                        SearchCategory.SONG -> "搜索歌曲或歌手"
                        SearchCategory.ALBUM -> "搜索专辑"
                        SearchCategory.NETEASE_PLAYLIST -> "搜索网易云歌单"
                    }
                )
            },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    enabled = keyword.isNotBlank() && !isSearching,
                    onClick = onSubmit
                ) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SearchCategory.entries.forEach { item ->
            val selected = item == category
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .clickable { onCategoryChange(item) }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeSingleLineText(
                    text = item.label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    minFontSize = 10.sp,
                    maxFontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showSources) {
        SourcePickerSheet(
            selectedValue = sourceValue,
            onDismiss = { showSources = false },
            onSelect = {
                showSources = false
                onSourceChange(it)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourcePickerSheet(
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
        ) {
            Text(
                text = "选择音源",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = supportedMusicSources,
                    key = { it.value }
                ) { source ->
                    ListItem(
                        headlineContent = { Text(source.label) },
                        supportingContent = if (source.recommended) {
                            { Text("推荐稳定源") }
                        } else {
                            null
                        },
                        trailingContent = if (source.value == selectedValue) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "当前音源",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(source.value) }
                    )
                }
            }
        }
    }
}

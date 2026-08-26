package com.diamond.gdapplication.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NeteasePlaylistScreen() {
    var playlistUrl by rememberSaveable {
        mutableStateOf("")
    }

    var message by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "网易歌单",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "粘贴网易云音乐歌单分享链接。",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = playlistUrl,
            onValueChange = {
                playlistUrl = it
                message = ""
            },
            label = {
                Text("网易云歌单链接")
            },
            placeholder = {
                Text("https://music.163.com/...")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        Button(
            enabled = playlistUrl.isNotBlank(),
            onClick = {
                message = "网易歌单解析功能尚未接入"
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("导入歌单")
        }

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Text(
            text = "GD 音乐台 API 暂不支持网易云歌单链接解析。",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}
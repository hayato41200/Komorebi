package com.example.Komorebi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.Komorebi.data.model.KonomiHistoryProgram

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchHistoryCard(
    history: KonomiHistoryProgram,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val program = history.program

    // 再生進捗の計算 (再生位置 / 総時間)
    // ※総時間は秒単位と仮定。KonomiTVのデータ構造に合わせて調整してください
    val progress = remember(history) {
        // 仮に1時間(3600秒)の番組として計算。実際はprogram.duration等を使用
        (history.playback_position / 3600.0).toFloat().coerceIn(0f, 1f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(240.dp) // 履歴カードは少しワイドに
            .height(140.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.DarkGray.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 番組情報の表示
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "前回視聴: ${history.last_watched_at}", // 本来は相対時間変換するとベター
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) Color.Black.copy(0.7f) else Color.White.copy(0.7f)
                )
            }

            // ★ 視聴進捗バー（一番下に配置）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(if (isFocused) Color(0xFFD32F2F) else Color.Red)
                )
            }
        }
    }
}

@Composable
fun HistoryRow(
    historyList: List<KonomiHistoryProgram>,
    onHistoryClick: (KonomiHistoryProgram) -> Unit,
    modifier: Modifier = Modifier // ★これを追加
) {
    if (historyList.isEmpty()) return

    // 受け取った modifier を一番親の Column に渡す
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = "視聴履歴",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(start = 32.dp, bottom = 12.dp)
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.5f)
        ) {
            items(historyList) { history ->
                WatchHistoryCard(
                    history = history,
                    onClick = { onHistoryClick(history) }
                )
            }
        }
    }
}
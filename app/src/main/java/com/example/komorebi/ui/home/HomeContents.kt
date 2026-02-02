package com.example.komorebi.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.komorebi.data.model.KonomiHistoryProgram
import com.example.komorebi.data.model.RecordedProgram
import com.example.komorebi.data.model.RecordedVideo
import com.example.komorebi.data.model.getThumbnailUrl
import com.example.komorebi.viewmodel.Channel
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeContents(
    lastWatchedChannel: Channel?,
    watchHistory: List<KonomiHistoryProgram>,
    onChannelClick: (Channel) -> Unit,
    onHistoryClick: (KonomiHistoryProgram) -> Unit,
    konomiIp: String,      // 追加
    konomiPort: String,    // 追加
    modifier: Modifier = Modifier,
    externalFocusRequester: FocusRequester
) {
    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(externalFocusRequester),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // --- 1. 前回視聴したチャンネル ---
        item {
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                SectionHeader(title = "前回視聴したチャンネル")
                Spacer(modifier = Modifier.height(12.dp))

                if (lastWatchedChannel != null) {
                    Surface(
                        onClick = { onChannelClick(lastWatchedChannel) },
                        modifier = Modifier.width(160.dp).height(80.dp),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(0.1f),
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(16.dp)) {
                            Text(text = lastWatchedChannel.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Surface(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.width(280.dp).height(80.dp),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(0.05f),
                            disabledContainerColor = Color.White.copy(0.05f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Text("最近視聴した番組はありません", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // --- 2. 録画視聴履歴 (サムネイル対応) ---
        item {
            Column {
                SectionHeader(title = "録画の視聴履歴", modifier = Modifier.padding(start = 32.dp, bottom = 12.dp))

                TvLazyRow(
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    pivotOffsets = PivotOffsets(parentFraction = 0.5f)
                ) {
                    if (watchHistory.isNotEmpty()) {
                        items(watchHistory) { history ->
                            WatchHistoryCard(
                                history = history,
                                konomiIp = konomiIp,
                                konomiPort = konomiPort,
                                onClick = { onHistoryClick(history) }
                            )
                        }
                    } else {
                        items(5) { DummyHistoryCard() }
                    }
                }
            }
        }
    }
}

/**
 * 視聴履歴のダミー用スケルトンカード
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DummyHistoryCard() {
    Surface(
        onClick = {},
        enabled = false,
        modifier = Modifier.width(240.dp).height(140.dp),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            disabledContainerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("", color = Color.DarkGray)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.9f),
        modifier = modifier
    )
}

//@OptIn(ExperimentalTvMaterial3Api::class)
//@Composable
//fun HistoryRow(
//    title: String,
//    historyList: List<KonomiHistoryProgram>,
//    onHistoryClick: (KonomiHistoryProgram) -> Unit
//) {
//    Column {
//        SectionHeader(title = title, modifier = Modifier.padding(start = 32.dp, bottom = 12.dp))
//        TvLazyRow(
//            contentPadding = PaddingValues(horizontal = 32.dp),
//            horizontalArrangement = Arrangement.spacedBy(16.dp),
//            pivotOffsets = PivotOffsets(parentFraction = 0.5f)
//        ) {
//            items(historyList) { history ->
//                WatchHistoryCard(history = history, onClick = { onHistoryClick(history) })
//            }
//        }
//    }
//}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchHistoryCard(
    history: KonomiHistoryProgram,
    konomiIp: String,
    konomiPort: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val program = history.program

    // サムネイルURL生成 (KonomiTV API)
    val thumbnailUrl = getThumbnailUrl(history.program.id, konomiIp, konomiPort)

    // 進捗計算
    val progress = remember(history) {
        try {
            val start = Instant.parse(program.start_time).epochSecond
            val end = Instant.parse(program.end_time).epochSecond
            val total = (end - start).toDouble()
            if (total > 0) (history.playback_position / total).toFloat().coerceIn(0f, 1f) else 0f
        } catch (e: Exception) {
            0f
        }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(140.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.DarkGray,
            focusedContainerColor = Color.White
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 背景サムネイル
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )

            // 2. グラデーションオーバーレイ (文字を見やすくするため、フォーカス時は薄くする)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isFocused) Color.White.copy(0.7f) else Color.Black.copy(0.8f)
                            )
                        )
                    )
            )

            // 3. テキストコンテンツ
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "続きから再生",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) Color.Black.copy(0.7f) else Color.White.copy(0.7f)
                )
            }

            // 4. 進捗バー (最下部)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Gray.copy(0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(if (isFocused) Color.Black else Color.Red)
                )
            }
        }
    }
}

/**
 * 拡張関数: 履歴データを再生用モデルに変換
 */
fun KonomiHistoryProgram.toRecordedProgram(): RecordedProgram {
    val programId = this.program.id.toIntOrNull() ?: 0
    return RecordedProgram(
        id = programId,
        title = this.program.title,
        description = this.program.description ?: "",
        startTime = this.program.start_time,
        endTime = this.program.end_time,
        duration = 0.0,
        isPartiallyRecorded = false,
        recordedVideo = RecordedVideo(
            id = programId,
            filePath = "", // 修正: エスケープを削除
            recordingStartTime = this.program.start_time,
            recordingEndTime = this.program.end_time,
            duration = 0.0,
            containerFormat = "",
            videoCodec = "",
            audioCodec = ""
        )
    )
}
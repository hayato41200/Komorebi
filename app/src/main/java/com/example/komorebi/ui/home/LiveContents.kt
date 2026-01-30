package com.example.komorebi.ui.home

import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.komorebi.buildStreamId
import com.example.komorebi.viewmodel.Channel
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveContent(
    groupedChannels: Map<String, List<Channel>>,
    lastWatchedChannel: Channel?,
    lastFocusedChannelId: String?,
    onFocusChannelChange: (String) -> Unit,
    mirakurunIp: String,
    mirakurunPort: String,
    topTabFocusRequester: FocusRequester,
    onChannelClick: (Channel) -> Unit
) {
    val categories = remember(groupedChannels) {
        groupedChannels.keys.map { key -> if (key == "GR") "地デジ" else key }
    }

    val listState = rememberTvLazyListState()
    var showList by remember { mutableStateOf(false) }

    // チャンネルIDごとにFocusRequesterを保持
    val channelFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // 共通タイマー（進捗バー用）
    var globalTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        showList = true
        while(true) {
            delay(10000)
            globalTick++
        }
    }

    // ★ フォーカスとスクロールの復元ロジック
    LaunchedEffect(showList) {
        if (showList) {
            val targetId = lastFocusedChannelId ?: lastWatchedChannel?.id

            if (targetId != null) {
                // 1. まず該当するカテゴリーまでスクロール（画面内にないとフォーカスできないため）
                val catIndex = categories.indexOfFirst { displayCat ->
                    val originalKey = if (displayCat == "地デジ") "GR" else displayCat
                    groupedChannels[originalKey]?.any { it.id == targetId } == true
                }

                if (catIndex >= 0) {
                    listState.scrollToItem(catIndex)
                }

                // 2. アニメーションとリスト構築を待ってからフォーカス要求
                delay(400)
                channelFocusRequesters[targetId]?.requestFocus()
            }
        }
    }

    AnimatedVisibility(
        visible = showList,
        enter = fadeIn(animationSpec = tween(500))
    ) {
        TvLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories, contentType = { "category" }) { displayCategory ->
                val originalKey = if (displayCategory == "地デジ") "GR" else displayCategory
                val channels = groupedChannels[originalKey] ?: emptyList()
                val categoryIndex = categories.indexOf(displayCategory)

                Column(modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false)) {
                    Text(
                        text = displayCategory,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                    )

                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pivotOffsets = PivotOffsets(parentFraction = 0.5f),
                        modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false)
                    ) {
                        items(
                            items = channels,
                            key = { it.id },
                            contentType = { "channel_card" }
                        ) { channel ->
                            // IDに紐づくFocusRequesterを取得
                            val fr = channelFocusRequesters.getOrPut(channel.id) { FocusRequester() }

                            ChannelWideCard(
                                channel = channel,
                                mirakurunIp = mirakurunIp,
                                mirakurunPort = mirakurunPort,
                                globalTick = globalTick,
                                onClick = { onChannelClick(channel) },
                                modifier = Modifier
                                    .focusRequester(fr)
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            onFocusChannelChange(channel.id)
                                        }
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                            if (categoryIndex == 0) {
                                                topTabFocusRequester.requestFocus()
                                                return@onKeyEvent true
                                            }
                                        }
                                        false
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelWideCard(
    channel: Channel,
    mirakurunIp: String,
    mirakurunPort: String,
    globalTick: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val progress = remember(channel.programPresent, globalTick) {
        calculateProgress(channel.programPresent?.startTime, channel.programPresent?.duration)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(150.dp)
            .height(78.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ロゴ表示（枠なし・元のサイズ）
                AsyncImage(
                    model = "http://$mirakurunIp:$mirakurunPort/api/services/${buildStreamId(channel)}/logo",
                    contentDescription = null,
                    modifier = Modifier.size(50.dp, 30.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        maxLines = 1,
                        color = if (isFocused) Color.Black.copy(0.7f) else Color.White.copy(0.7f)
                    )

                    Text(
                        text = channel.programPresent?.title ?: "放送休止中",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = if (isFocused) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = if (isFocused) Modifier.basicMarquee() else Modifier
                    )
                }
            }

            if (channel.programPresent != null) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(0.1f))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (isFocused) Color(0xFFD32F2F) else Color.White))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun calculateProgress(startTimeString: String?, duration: Int?): Float {
    if (startTimeString == null || duration == null || duration <= 0) return 0f
    return try {
        val startTimeMillis = ZonedDateTime.parse(startTimeString).toInstant().toEpochMilli()
        val currentTimeMillis = System.currentTimeMillis()
        val progress = (currentTimeMillis - startTimeMillis).toFloat() / (duration * 1000).toFloat()
        progress.coerceIn(0f, 1f)
    } catch (e: Exception) {
        0f
    }
}
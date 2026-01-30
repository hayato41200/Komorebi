package com.example.komorebi.ui.home

import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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

    // ★ 追加：各カテゴリーの横スクロール状態を保持するMap
    // 本来は一階層上のScreenやViewModelで保持して渡すと、タブ切り替えでもリセットされなくなります
    val rowStates = remember { mutableMapOf<String, androidx.tv.foundation.lazy.list.TvLazyListState>() }

    val targetId = lastFocusedChannelId ?: lastWatchedChannel?.id
    val initialCategoryIndex = remember(categories, targetId) {
        if (targetId == null) 0 else {
            categories.indexOfFirst { displayCat ->
                val originalKey = if (displayCat == "地デジ") "GR" else displayCat
                groupedChannels[originalKey]?.any { it.id == targetId } == true
            }.coerceAtLeast(0)
        }
    }

    val listState = rememberTvLazyListState(initialFirstVisibleItemIndex = initialCategoryIndex)
    var showList by remember { mutableStateOf(false) }
    val channelFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // ... (Tickロジックはそのまま) ...
    var globalTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        showList = true
        while(true) {
            delay(10000)
            globalTick++
        }
    }

    // ★ 修正：フォーカス復元ロジックの強化
    LaunchedEffect(showList) {
        if (showList && targetId != null) {
            delay(150) // 少し短縮

            // 1. カテゴリーの縦位置を復元
            if (listState.firstVisibleItemIndex != initialCategoryIndex) {
                listState.scrollToItem(initialCategoryIndex)
            }

            // 2. 該当チャンネルの横位置を特定し、スクロールを命令
            val displayCategory = categories[initialCategoryIndex]
            val originalKey = if (displayCategory == "地デジ") "GR" else displayCategory
            val channelIndex = groupedChannels[originalKey]?.indexOfFirst { it.id == targetId } ?: -1

            if (channelIndex >= 0) {
                // そのカテゴリーの横スクロール状態を取得してスクロール
                rowStates[displayCategory]?.scrollToItem(channelIndex)
            }

            delay(100)
            channelFocusRequesters[targetId]?.requestFocus()
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.3f)
        ) {
            items(categories, key = { it }) { displayCategory ->
                val originalKey = if (displayCategory == "地デジ") "GR" else displayCategory
                val channels = groupedChannels[originalKey] ?: emptyList()
                val categoryIndex = categories.indexOf(displayCategory)

                // ★ カテゴリーごとのTvLazyListStateを取得または生成
                val rowState = rowStates.getOrPut(displayCategory) {
                    // 戻ってきたときにそのチャンネルが画面内にくるよう初期値を計算
                    val cIndex = if (targetId != null) channels.indexOfFirst { it.id == targetId } else -1
                    androidx.tv.foundation.lazy.list.rememberTvLazyListState(
                        initialFirstVisibleItemIndex = if (cIndex >= 0) cIndex else 0
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false)) {
                    Text(
                        text = displayCategory,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                    )

                    TvLazyRow(
                        state = rowState, // ★ 横位置の状態を適用
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pivotOffsets = PivotOffsets(parentFraction = 0.5f),
                        modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false)
                    ) {
                        items(
                            items = channels,
                            key = { it.id }
                        ) { channel ->
                            val fr = channelFocusRequesters.getOrPut(channel.id) { FocusRequester() }

                            ChannelWideCard(
                                channel = channel,
                                // ... (引数はそのまま) ...
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

    // スケールと光を滑らかに同期させる
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(250), // 250msがTV操作で最も心地よい速度です
        label = "ScaleAnimation"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(250),
        label = "GlowAlpha"
    )

    val progress = remember(channel.programPresent, globalTick) {
        calculateProgress(channel.programPresent?.startTime, channel.programPresent?.duration)
    }

    // Surface自体に drawBehind を持たせることでレイアウトへの影響をゼロにします
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(160.dp) // 幅を微調整
            .height(84.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        // カードのサイズに合わせた光の範囲
                        radius = size.width * 0.9f
                    )
                }
            }
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        // デフォルトのScaleは graphicsLayer で制御するため固定にします
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "http://$mirakurunIp:$mirakurunPort/api/services/${buildStreamId(channel)}/logo",
                    contentDescription = null,
                    modifier = Modifier.size(42.dp, 28.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        color = if (isFocused) Color.Black.copy(0.6f) else Color.White.copy(0.5f)
                    )

                    Text(
                        text = channel.programPresent?.title ?: "放送休止中",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = if (isFocused) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = if (isFocused) Modifier.basicMarquee() else Modifier
                    )
                }
            }

            if (channel.programPresent != null) {
                Box(modifier = Modifier.fillMaxWidth().height(3.5.dp).background(Color.Gray.copy(0.1f))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(if (isFocused) Color(0xFFE53935) else Color.White.copy(alpha = 0.8f))
                    )
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
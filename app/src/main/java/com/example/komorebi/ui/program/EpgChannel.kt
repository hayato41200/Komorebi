package com.example.komorebi.ui.program

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
// ★ 重要: TV Material 3 の Border をインポート
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import coil.compose.AsyncImage
import com.example.komorebi.data.model.EpgChannel
import com.example.komorebi.data.model.EpgChannelWrapper
import com.example.komorebi.data.model.EpgProgram
import com.example.komorebi.data.util.EpgUtils
import com.example.komorebi.viewmodel.EpgUiState
import com.example.komorebi.viewmodel.EpgViewModel
import java.time.Duration
import java.time.OffsetDateTime

private const val DP_PER_MINUTE = 1.3f
private val HOUR_HEIGHT = (60 * DP_PER_MINUTE).dp

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpgScreen(
    viewModel: EpgViewModel,
    topTabFocusRequester: FocusRequester,
    tabFocusRequester: FocusRequester, // 放送波タブ用のRequester
    firstCellFocusRequester: FocusRequester // 番組セル用のRequester
) {
    val uiState = viewModel.uiState
    val baseTime = remember { OffsetDateTime.now().withMinute(0).withSecond(0).withNano(0) }
    var selectedBroadcastingType by remember { mutableStateOf("GR") }

    val groupedChannels = remember(uiState) {
        if (uiState is EpgUiState.Success) {
            uiState.data.groupBy { it.channel.type ?: "UNKNOWN" }
        } else {
            emptyMap()
        }
    }

    val categories = remember(groupedChannels) {
        groupedChannels.keys.toList()
    }

    var isTabFocused by remember { mutableStateOf(false) }
    var isGridFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            isGridFocused -> tabFocusRequester.requestFocus()
            isTabFocused -> topTabFocusRequester.requestFocus()
            else -> topTabFocusRequester.requestFocus()
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // 放送波切替タブ
        BroadcastingTypeTabs(
            selectedType = selectedBroadcastingType,
            onTypeSelected = { selectedBroadcastingType = it },
            tabFocusRequester = tabFocusRequester,
            firstCellFocusRequester = firstCellFocusRequester, // 追加
            categories = categories,
            modifier = Modifier.onFocusChanged { isTabFocused = it.hasFocus }
        )

        when (uiState) {
            is EpgUiState.Success -> {
                val displayChannels = groupedChannels[selectedBroadcastingType] ?: emptyList()
                Box(modifier = Modifier.weight(1f).onFocusChanged { isGridFocused = it.hasFocus }) {
                    EpgGrid(
                        channels = displayChannels,
                        baseTime = baseTime,
                        viewModel = viewModel,
                        tabFocusRequester = tabFocusRequester,
                        firstCellFocusRequester = firstCellFocusRequester
                    )
                }
            }
            is EpgUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading...", color = Color.White)
                }
            }
            is EpgUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("エラーが発生しました", color = Color.Red)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpgGrid(
    channels: List<EpgChannelWrapper>,
    baseTime: OffsetDateTime,
    viewModel: EpgViewModel,
    tabFocusRequester: FocusRequester,
    firstCellFocusRequester: FocusRequester
) {
    val now = OffsetDateTime.now()
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    fun getNowOffsetMinutes() = Duration.between(baseTime, OffsetDateTime.now()).toMinutes()
    var nowOffsetMinutes by remember { mutableStateOf(getNowOffsetMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            nowOffsetMinutes = getNowOffsetMinutes()
        }
    }

    val channelWidth = 150.dp
    val timeColumnWidth = 55.dp
    val headerHeight = 55.dp
    val totalGridWidth = channelWidth * channels.size

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(timeColumnWidth).height(headerHeight).background(Color(0xFF111111)))
            Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                channels.forEach { wrapper ->
                    ChannelHeaderCell(wrapper.channel, channelWidth, headerHeight, viewModel.getMirakurunLogoUrl(wrapper.channel))
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.width(timeColumnWidth).verticalScroll(verticalScrollState).background(Color(0xFF111111))) {
                TimeColumnContent(baseTime)
            }

            Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState).verticalScroll(verticalScrollState)) {
                Row {
                    channels.forEachIndexed { channelIndex, channelWrapper ->
                        Box(modifier = Modifier.width(channelWidth).height(HOUR_HEIGHT * 24)) {
                            channelWrapper.programs.forEachIndexed { programIndex, program ->
                                val isFirstEverCell = channelIndex == 0 && programIndex == 0
                                CompactProgramCell(
                                    epgProgram = program,
                                    baseTime = baseTime,
                                    now = now,
                                    width = channelWidth,
                                    tabFocusRequester = tabFocusRequester,
                                    columnIndex = channelIndex,
                                    totalColumns = channels.size,
                                    modifier = if (isFirstEverCell) Modifier.focusRequester(firstCellFocusRequester) else Modifier
                                )
                            }
                        }
                    }
                }

                val lineOffset = (nowOffsetMinutes * DP_PER_MINUTE).dp
                Box(modifier = Modifier.width(totalGridWidth).offset(y = lineOffset)) {
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Red))
                    Canvas(modifier = Modifier.size(8.dp).offset(y = (-3).dp)) {
                        drawCircle(color = Color.Red, radius = size.width / 2)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CompactProgramCell(
    epgProgram: EpgProgram,
    baseTime: OffsetDateTime,
    now: OffsetDateTime,
    width: Dp,
    tabFocusRequester: FocusRequester,
    columnIndex: Int,
    totalColumns: Int,
    modifier: Modifier = Modifier
) {
    val startTime = remember(epgProgram.start_time) { OffsetDateTime.parse(epgProgram.start_time) }
    val endTime = remember(startTime, epgProgram.duration) { startTime.plusSeconds(epgProgram.duration.toLong()) }
    if (endTime.isBefore(baseTime)) return

    val minutesFromBase = Duration.between(baseTime, startTime).toMinutes()
    val topOffset = (minutesFromBase * DP_PER_MINUTE).dp
    val cellHeight = (epgProgram.duration / 60 * DP_PER_MINUTE).dp

    var isFocused by remember { mutableStateOf(false) }
    val isPast = endTime.isBefore(now)
    val expandedMinHeight = 120.dp

    val isAtTop = topOffset <= 0.dp
    val visualTopOffset = topOffset.coerceAtLeast(0.dp)
    val focusableHeight = if (topOffset < 0.dp) (cellHeight + topOffset).coerceAtLeast(0.dp) else cellHeight

    Box(
        modifier = modifier
            .offset(y = visualTopOffset)
            .width(width)
            .height(focusableHeight)
            .zIndex(if (isFocused) 100f else 1f)
            .focusProperties {
                if (columnIndex == 0) left = FocusRequester.Cancel
                if (columnIndex == totalColumns - 1) right = FocusRequester.Cancel
            }
            .onPreviewKeyEvent { event ->
                if (isAtTop && event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown) {
                    tabFocusRequester.requestFocus()
                    true
                } else false
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .animateContentSize()
                .background(if (isPast) Color(0xFF151515) else Color(0xFF222222))
                .then(
                    if (isFocused) {
                        Modifier.heightIn(min = expandedMinHeight).border(2.dp, Color.White, RectangleShape).shadow(12.dp)
                    } else {
                        Modifier.height(focusableHeight).border(0.5.dp, Color(0xFF333333), RectangleShape)
                    }
                )
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(if (isPast) Color.Gray else EpgUtils.getGenreColor(epgProgram.majorGenre)))
                Column(modifier = Modifier.padding(6.dp).fillMaxWidth()) {
                    val alpha = if (isPast) 0.5f else 1.0f
                    Text(text = EpgUtils.formatTime(epgProgram.start_time), fontSize = 10.sp, color = Color.LightGray.copy(alpha = alpha), fontWeight = FontWeight.Bold)
                    Text(
                        text = epgProgram.title,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = alpha),
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = if (isFocused) 5 else if (cellHeight > 30.dp) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    if (isFocused) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = epgProgram.description ?: "", fontSize = 10.sp, color = Color.LightGray, lineHeight = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelHeaderCell(channel: EpgChannel, width: Dp, height: Dp, logoUrl: String) {
    Surface(
        onClick = {},
        modifier = Modifier.width(width).height(height),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF111111),
            contentColor = Color.White
        ),
        // ★修正ポイント: border を Border オブジェクトとして定義して渡す
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(0.5.dp, Color(0xFF333333))
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(width = 36.dp, height = 20.dp).clip(RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(model = logoUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = channel.channel_number ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = channel.name, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeColumnContent(baseTime: OffsetDateTime) {
    val dayOfWeekJapan = listOf("日", "月", "火", "水", "木", "金", "土")
    repeat(24) { hourOffset ->
        val displayTime = baseTime.plusHours(hourOffset.toLong())
        Box(
            modifier = Modifier.height(HOUR_HEIGHT).fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = Color(0xFF222222),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (displayTime.hour == 0) {
                    val dayOfWeekIndex = displayTime.dayOfWeek.value % 7
                    val dayColor = when (dayOfWeekIndex) {
                        0 -> Color(0xFFFF5252)
                        6 -> Color(0xFF448AFF)
                        else -> Color.White
                    }
                    Text(text = "${displayTime.monthValue}/${displayTime.dayOfMonth}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "(${dayOfWeekJapan[dayOfWeekIndex]})", color = dayColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "${displayTime.hour}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(text = "時", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun BroadcastingTypeTabs(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    tabFocusRequester: FocusRequester,
    firstCellFocusRequester: FocusRequester,
    categories: List<String>,
    modifier: Modifier = Modifier
) {
    val typeLabels = mapOf("GR" to "地デジ", "BS" to "BS", "CS" to "CS", "BS4K" to "BS4K", "SKY" to "スカパー")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEachIndexed { index, code ->
            val label = typeLabels[code] ?: code
            val isSelected = selectedType == code

            Surface(
                onClick = { onTypeSelected(code) },
                modifier = Modifier
                    .width(120.dp)
                    .height(42.dp)
                    .padding(horizontal = 4.dp)
                    .focusRequester(if (index == 0) tabFocusRequester else FocusRequester())
                    .focusProperties {
                        down = firstCellFocusRequester
                    },
                shape = ClickableSurfaceDefaults.shape(RectangleShape),
                colors = ClickableSurfaceDefaults.colors(
                    // 通常時（非フォーカス）の背景
                    containerColor = Color.Transparent,
                    // フォーカス時の背景
                    focusedContainerColor = Color.White,
                    // ★ 修正: 非フォーカス時の文字色を「isSelected」に基づいて明示的に指定
                    contentColor = if (isSelected) Color.White else Color.Gray,
                    // ★ 修正: フォーカス時の文字色（黒）
                    focusedContentColor = Color.Black
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f)
            ) {
                // Surface内部で LocalContentColor が更新される
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            // ★ 修正: Surfaceから提供される LocalContentColor を使う
                            // これで focusedContentColor や contentColor が自動適用される
                            color = androidx.tv.material3.LocalContentColor.current
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    // 下線も文字色と連動させる
                                    .background(androidx.tv.material3.LocalContentColor.current)
                            )
                        }
                    }
                }
            }
        }
    }
}
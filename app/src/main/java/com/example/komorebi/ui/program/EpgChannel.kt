package com.example.komorebi.ui.program

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import coil.compose.AsyncImage
import com.example.komorebi.data.model.EpgChannel
import com.example.komorebi.data.model.EpgChannelWrapper
import com.example.komorebi.data.model.EpgProgram
import com.example.komorebi.data.util.EpgUtils
import com.example.komorebi.viewmodel.EpgUiState
import com.example.komorebi.viewmodel.EpgViewModel
import java.time.Duration
import java.time.OffsetDateTime

// 1分あたりの高さ設定
private const val DP_PER_MINUTE = 1.3f
private val HOUR_HEIGHT = (60 * DP_PER_MINUTE).dp

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpgScreen(
    viewModel: EpgViewModel,
    tabFocusRequester: FocusRequester,
    firstCellFocusRequester: FocusRequester
) {
    val uiState = viewModel.uiState //
    // 基準時刻の保持
    val baseTime = remember { OffsetDateTime.now().withMinute(0).withSecond(0).withNano(0) }

    LaunchedEffect(Unit) {
        viewModel.loadEpg() //
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // 放送波タブ (常に表示される安定版の状態)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF1A1A1A))
                .focusRequester(tabFocusRequester)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Text("地デジ | BS | CS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        when (uiState) {
            is EpgUiState.Loading -> { //
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("読み込み中...", color = Color.White)
                }
            }
            is EpgUiState.Success -> { //
                EpgGrid(
                    channels = uiState.data,
                    baseTime = baseTime,
                    viewModel = viewModel,
                    tabFocusRequester = tabFocusRequester,
                    firstCellFocusRequester = firstCellFocusRequester
                )
            }
            is EpgUiState.Error -> { //
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("エラー: ${uiState.message}", color = Color.Red)
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
    val now = OffsetDateTime.now() //
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // 現在時刻の赤線位置を計算
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
        // チャンネルヘッダー行
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(timeColumnWidth).height(headerHeight).background(Color(0xFF111111)))
            Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                channels.forEach { wrapper ->
                    ChannelHeaderCell(wrapper.channel, channelWidth, headerHeight, viewModel.getMirakurunLogoUrl(wrapper.channel))
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // 時刻カラム
            Column(
                modifier = Modifier.width(timeColumnWidth).verticalScroll(verticalScrollState).background(Color(0xFF111111))
            ) {
                TimeColumnContent(baseTime)
            }

            // 番組グリッド本体
            Box(
                modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState).verticalScroll(verticalScrollState)
            ) {
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

                // 現在時刻の線
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
    val startTime = remember(epgProgram.start_time) { OffsetDateTime.parse(epgProgram.start_time) } //
    val endTime = remember(startTime, epgProgram.duration) { startTime.plusSeconds(epgProgram.duration.toLong()) } //
    if (endTime.isBefore(baseTime)) return //

    val minutesFromBase = Duration.between(baseTime, startTime).toMinutes()
    val topOffset = (minutesFromBase * DP_PER_MINUTE).dp
    val cellHeight = (epgProgram.duration / 60 * DP_PER_MINUTE).dp

    var isFocused by remember { mutableStateOf(false) }
    val isPast = endTime.isBefore(now) //
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
            .onFocusChanged { isFocused = it.isFocused } //
            .focusable()
            .clickable { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .animateContentSize() //
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
                // ジャンルカラー
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

// チャンネルヘッダーと時刻軸は変更なし
@Composable
fun ChannelHeaderCell(channel: EpgChannel, width: Dp, height: Dp, logoUrl: String) {
    val typeLabel = remember(channel.type) {
        when (channel.type?.uppercase()) {
            "GR" -> "地デジ"
            "BS" -> "BS"
            "CS" -> "CS"
            else -> "TV"
        }
    }
    Surface(
        modifier = Modifier.width(width).height(height),
        color = Color(0xFF111111),
        border = BorderStroke(0.5.dp, Color(0xFF333333))
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 44.dp, height = 25.dp).clip(RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = logoUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = channel.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "$typeLabel ${channel.channel_number}", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeColumnContent(baseTime: OffsetDateTime) {
    repeat(24) { hourOffset ->
        val displayTime = baseTime.plusHours(hourOffset.toLong())
        Box(
            modifier = Modifier.height(HOUR_HEIGHT).fillMaxWidth().drawBehind {
                drawLine(Color(0xFF222222), androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx())
            },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (displayTime.hour == 0) Text("${displayTime.monthValue}/${displayTime.dayOfMonth}", color = Color.Gray, fontSize = 8.sp)
                Text("${displayTime.hour}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("時", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}
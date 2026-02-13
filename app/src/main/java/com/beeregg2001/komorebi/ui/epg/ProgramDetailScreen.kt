package com.beeregg2001.komorebi.ui.epg

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.beeregg2001.komorebi.data.model.EpgProgram
import com.beeregg2001.komorebi.data.repository.TaskErrorType
import com.beeregg2001.komorebi.ui.theme.NotoSansJP
import com.beeregg2001.komorebi.viewmodel.ReservationTaskUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@androidx.annotation.OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    program: EpgProgram,
    reservationState: ReservationTaskUiState,
    onPlayClick: (EpgProgram) -> Unit,
    onRecordClick: (EpgProgram) -> Unit,
    onRetryRecordClick: (EpgProgram) -> Unit,
    onBackClick: () -> Unit,
    initialFocusRequester: FocusRequester
) {
    val now = OffsetDateTime.now()
    val startTime = OffsetDateTime.parse(program.start_time)
    val endTime = OffsetDateTime.parse(program.end_time)

    val isPast = endTime.isBefore(now)
    val isBroadcasting = now.isAfter(startTime) && now.isBefore(endTime)
    val isFuture = startTime.isAfter(now)
    val reservationButtonText = if (reservationState.isReserved) "予約解除" else "予約する"

    var isReady by remember { mutableStateOf(false) }
    var isClickEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(program) {
        isClickEnabled = false
        yield()
        try {
            initialFocusRequester.requestFocus()
        } catch (e: Exception) {
            Log.e("Detail", "Focus request failed", e)
        }
        isReady = true
        delay(500)
        isClickEnabled = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .focusGroup()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors = listOf(Color(0xFF1E1E1E), Color.Black)))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (isBroadcasting) {
                    Button(
                        onClick = { if (isClickEnabled) onPlayClick(program) },
                        modifier = Modifier.fillMaxWidth().focusRequester(initialFocusRequester)
                    ) {
                        Text("視聴する", fontFamily = NotoSansJP, fontWeight = FontWeight.Bold)
                    }
                } else if (isFuture) {
                    Button(
                        onClick = { if (isClickEnabled && !reservationState.isLoading) onRecordClick(program) },
                        enabled = !reservationState.isLoading,
                        modifier = Modifier.fillMaxWidth().focusRequester(initialFocusRequester)
                    ) {
                        Text(
                            if (reservationState.isLoading) "処理中..." else reservationButtonText,
                            fontFamily = NotoSansJP,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (reservationState.errorType != null) {
                        OutlinedButton(
                            onClick = { if (isClickEnabled && !reservationState.isLoading) onRetryRecordClick(program) },
                            enabled = !reservationState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("リトライ", fontFamily = NotoSansJP)
                        }
                        Text(
                            text = buildReservationErrorMessage(reservationState.errorType, reservationState.errorDetail),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB3B3),
                            fontFamily = NotoSansJP,
                            lineHeight = 18.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = { if (isClickEnabled) onBackClick() },
                    modifier = Modifier.fillMaxWidth().then(if (isPast) Modifier.focusRequester(initialFocusRequester) else Modifier)
                ) {
                    Text("戻る", fontFamily = NotoSansJP)
                }
            }

            Spacer(modifier = Modifier.width(56.dp))

            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.value + 300) }
                                    true
                                }

                                Key.DirectionUp -> {
                                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.value - 300) }
                                    true
                                }

                                else -> false
                            }
                        } else false
                    }
                    .focusable()
                    .verticalScroll(scrollState)
            ) {
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd(E) HH:mm")
                Text(
                    text = "${startTime.format(formatter)} ～ ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.LightGray,
                    fontFamily = NotoSansJP
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, lineHeight = 46.sp),
                    color = Color.White,
                    fontFamily = NotoSansJP
                )

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "番組概要",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontFamily = NotoSansJP,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = program.description ?: "説明はありません。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                    fontFamily = NotoSansJP,
                    lineHeight = 28.sp
                )

                if (isReady) ProgramDetailedInfo(program)
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

private fun buildReservationErrorMessage(type: TaskErrorType, detail: String?): String {
    val base = when (type) {
        TaskErrorType.TUNER_SHORTAGE -> "チューナー不足のため予約できません。"
        TaskErrorType.DUPLICATED -> "重複予約のため予約できません。"
        TaskErrorType.NETWORK -> "オフライン中です。接続回復後に再試行してください。"
        TaskErrorType.UNKNOWN -> "予約処理に失敗しました。"
    }
    return if (detail.isNullOrBlank()) base else "$base\n$detail"
}

@Composable
fun ProgramDetailedInfo(program: EpgProgram) {
    Column {
        program.detail?.forEach { (label, content) ->
            if (content.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontFamily = NotoSansJP,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    fontFamily = NotoSansJP,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

package com.beeregg2001.komorebi.ui.epg

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.zIndex
import androidx.tv.material3.*
import com.beeregg2001.komorebi.data.model.EpgProgram
import com.beeregg2001.komorebi.viewmodel.EpgUiState
import com.beeregg2001.komorebi.viewmodel.EpgViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpgNavigationContainer(
    uiState: EpgUiState,
    logoUrls: List<String>,
    mirakurunIp: String,
    mirakurunPort: String,
    mainTabFocusRequester: FocusRequester,
    contentRequester: FocusRequester,
    selectedProgram: EpgProgram?,
    onProgramSelected: (EpgProgram?) -> Unit,
    isJumpMenuOpen: Boolean,
    onJumpMenuStateChanged: (Boolean) -> Unit,
    onNavigateToPlayer: (String, String, String) -> Unit,
    currentType: String,
    onTypeChanged: (String) -> Unit,
    restoreChannelId: String? = null,
    availableTypes: List<String> = emptyList(),
    epgViewModel: EpgViewModel
) {
    var jumpTargetTime by remember { mutableStateOf<OffsetDateTime?>(null) }
    var internalRestoreChannelId by remember { mutableStateOf(restoreChannelId) }
    var internalRestoreStartTime by remember { mutableStateOf<String?>(null) }

    // ★修正: 親から渡される restoreChannelId を監視し、状態を同期
    // null が渡された（他タブからの移動）場合は、内部の復元用情報もクリアする
    LaunchedEffect(restoreChannelId) {
        internalRestoreChannelId = restoreChannelId
        if (restoreChannelId == null) {
            internalRestoreStartTime = null
        }
    }

    val currentLogoUrls = remember(logoUrls) {
        if (logoUrls.isNotEmpty()) logoUrls else emptyList()
    }
    val displayLogoUrls = if (logoUrls.isNotEmpty()) logoUrls else currentLogoUrls

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Menu -> {
                            onJumpMenuStateChanged(!isJumpMenuOpen)
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        ModernEpgCanvasEngine_Smooth(
            uiState = uiState,
            logoUrls = displayLogoUrls,
            topTabFocusRequester = mainTabFocusRequester,
            contentFocusRequester = contentRequester,
            onProgramSelected = onProgramSelected,
            jumpTargetTime = jumpTargetTime,
            onJumpFinished = { jumpTargetTime = null },
            onEpgJumpMenuStateChanged = onJumpMenuStateChanged,
            currentType = currentType,
            onTypeChanged = onTypeChanged,
            restoreChannelId = internalRestoreChannelId,
            restoreProgramStartTime = internalRestoreStartTime,
            availableTypes = availableTypes
        )

        if (selectedProgram != null) {
            val detailInitialFocusRequester = remember { FocusRequester() }
            val reservationState by epgViewModel.observeProgramTask(selectedProgram.id).collectAsState(initial = com.beeregg2001.komorebi.viewmodel.ReservationTaskUiState())
            val scope = rememberCoroutineScope()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .zIndex(2f)
            ) {
                ProgramDetailScreen(
                    program = selectedProgram,
                    reservationState = reservationState,
                    onPlayClick = { program ->
                        onNavigateToPlayer(program.channel_id, mirakurunIp, mirakurunPort)
                    },
                    onRecordClick = { program ->
                        scope.launch {
                            epgViewModel.toggleProgramReservation(
                                programId = program.id,
                                channelId = program.channel_id,
                                title = program.title,
                                startAt = program.start_time,
                                endAt = program.end_time
                            )
                        }
                    },
                    onRetryRecordClick = { program ->
                        scope.launch {
                            epgViewModel.toggleProgramReservation(
                                programId = program.id,
                                channelId = program.channel_id,
                                title = program.title,
                                startAt = program.start_time,
                                endAt = program.end_time
                            )
                        }
                    },
                    onBackClick = {
                        // 詳細から戻った時は、元の番組セルにフォーカスを戻す
                        runCatching { contentRequester.requestFocus() }
                        internalRestoreChannelId = selectedProgram.channel_id
                        internalRestoreStartTime = selectedProgram.start_time
                        onProgramSelected(null)
                    },
                    initialFocusRequester = detailInitialFocusRequester
                )
            }
            LaunchedEffect(selectedProgram) {
                yield()
                // 詳細表示時の初期フォーカスを安全に要求
                runCatching { detailInitialFocusRequester.requestFocus() }
            }
        }

        // ★修正: 復元IDがある場合のみ（再生終了後など）フォーカスを本体へ移動
        // これにより、通常のタブ切り替え時にはフォーカスがトップナビに残ります
        LaunchedEffect(internalRestoreChannelId) {
            if (internalRestoreChannelId != null) {
                delay(100)
                runCatching { contentRequester.requestFocus() }
            }
        }

        AnimatedVisibility(
            visible = isJumpMenuOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(10f)
        ) {
            val now = remember { OffsetDateTime.now() }
            val dates = remember(now) { List(7) { now.plusDays(it.toLong()) } }
            EpgJumpMenu(
                dates = dates,
                onSelect = { selectedTime ->
                    jumpTargetTime = selectedTime
                    onJumpMenuStateChanged(false)
                },
                onDismiss = { onJumpMenuStateChanged(false) }
            )
        }
    }
}
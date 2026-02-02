package com.example.komorebi

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.*
import com.example.komorebi.ui.theme.KomorebiTheme
import com.example.komorebi.ui.theme.SettingsScreen
import com.example.komorebi.data.SettingsRepository
import com.example.komorebi.ui.components.ExitDialog
import com.example.komorebi.ui.home.HomeLauncherScreen
import com.example.komorebi.ui.home.LoadingScreen
import com.example.komorebi.ui.live.LivePlayerScreen
import com.example.komorebi.viewmodel.Channel
import com.example.komorebi.viewmodel.ChannelViewModel
import com.example.komorebi.viewmodel.EpgViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ChannelViewModel by viewModels()
    private val epgViewModel: EpgViewModel by viewModels()
    private val channelViewModel: ChannelViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.fetchChannels()
        epgViewModel.preloadAllEpgData()
        channelViewModel.fetchRecentRecordings()

        setContent {
            KomorebiTheme {
                val context = LocalContext.current
                val activity = context as? Activity
                var isUiReady by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                val isChannelLoading by viewModel.isLoading.collectAsState()
                val isEpgLoading by epgViewModel.isPreloading.collectAsState()
                val groupedChannels by viewModel.groupedChannels.collectAsState()

                // --- 状態保持 ---
                var selectedChannel by remember { mutableStateOf<Channel?>(null) }
                var isSettingsMode by remember { mutableStateOf(false) }
                var isMiniListOpen by remember { mutableStateOf(false) }
                var showExitDialog by remember { mutableStateOf(false) }

                // タブの状態を MainActivity で一括管理し、戻ってきた時に保持されるようにします
                var currentTabIndex by remember { mutableIntStateOf(0) }

                val isDataLoading = isChannelLoading || isEpgLoading
                val extraDelay = getDynamicDelay()

                val repository = remember { SettingsRepository(context) }
                val mirakurunIp by repository.mirakurunIp.collectAsState(initial = "192.168.100.60")
                val mirakurunPort by repository.mirakurunPort.collectAsState(initial = "40772")
                val konomiIp by repository.konomiIp.collectAsState(initial = "https://192-168-100-60.local.konomi.tv")
                val konomiPort by repository.konomiPort.collectAsState(initial = "7000")

                // ★ BackHandler：最優先（視聴中）から順に判定
                BackHandler(enabled = true) {
                    when {
                        // 1. ライブ視聴中なら視聴を終了（selectedChannelをnullにすればHomeに戻る）
                        selectedChannel != null -> {
                            selectedChannel = null
                        }
                        // 2. 設定画面なら設定を閉じる
                        isSettingsMode -> {
                            isSettingsMode = false
                        }
                        // 3. それ以外（Home画面）は HomeLauncherScreen 側の BackHandler に任せるため
                        // 本来はここでは何もしないか、ダイアログ表示。
                        // HomeLauncher側でBackHandlerがあるため、こちらはHome時はダイアログ表示に統一
                        else -> {
                            showExitDialog = true
                        }
                    }
                }

                if (isDataLoading) {
                    LoadingScreen()
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            selectedChannel != null -> {
                                key(selectedChannel!!.id) {
                                    LivePlayerScreen(
                                        channel = selectedChannel!!,
                                        groupedChannels = groupedChannels,
                                        mirakurunIp = mirakurunIp,
                                        mirakurunPort = mirakurunPort,
                                        isMiniListOpen = isMiniListOpen,
                                        onMiniListToggle = { isMiniListOpen = it },
                                        onChannelSelect = { selectedChannel = it },
                                        onBackPressed = {
                                            selectedChannel = null
                                        }
                                    )
                                }
                            }
                            isSettingsMode -> {
                                SettingsScreen(onBack = { isSettingsMode = false })
                            }
                            else -> {
                                HomeLauncherScreen(
                                    groupedChannels = groupedChannels,
                                    lastWatchedChannel = selectedChannel,
                                    mirakurunIp = mirakurunIp,
                                    mirakurunPort = mirakurunPort,
                                    konomiIp = konomiIp,
                                    konomiPort = konomiPort,
                                    initialTabIndex = currentTabIndex,
                                    // タブ変更イベントを拾って MainActivity の currentTabIndex を更新
                                    onTabChange = { currentTabIndex = it },
                                    onChannelClick = { channel ->
                                        selectedChannel = channel
                                        // ★修正ポイント: ここで強制的に 1 にしていたのを削除
                                        // これにより、番組表(2)から遷移した場合は 2 のまま保持されます
                                    },
                                    onUiReady = {
                                        scope.launch {
                                            delay(extraDelay)
                                            isUiReady = true
                                        }
                                    }
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isUiReady,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LoadingScreen()
                            }
                        }
                    }
                }

                if (showExitDialog) {
                    ExitDialog(onConfirm = { activity?.finish() }, onDismiss = { showExitDialog = false })
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun getDynamicDelay(): Long {
    val context = LocalContext.current
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)
    return when {
        totalRamGb <= 1.5 -> 7000L
        totalRamGb <= 3.0 -> 4500L
        else -> 1000L
    }
}
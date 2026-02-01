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
    private val epgViewModel: EpgViewModel by viewModels() // ★追加

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 両方のデータをフェッチ開始
        viewModel.fetchChannels()
        epgViewModel.preloadAllEpgData() // ★番組表のプリロード開始

        setContent {
            KomorebiTheme {
                val context = LocalContext.current
                val activity = context as? Activity
                // 1. Compose側の準備完了を管理する状態を追加
                var isUiReady by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                // 放送局リストのロード状態
                val isChannelLoading by viewModel.isLoading.collectAsState()
                // 番組表のプリロード状態
                val isEpgLoading by epgViewModel.isPreloading.collectAsState()

                val groupedChannels by viewModel.groupedChannels.collectAsState()

                // --- 状態保持（変更なし） ---
                var selectedChannel by remember { mutableStateOf<Channel?>(null) }
                var isPlayerMode by remember { mutableStateOf(false) }
                var isSettingsMode by remember { mutableStateOf(false) }
                var isMiniListOpen by remember { mutableStateOf(false) }
                var showExitDialog by remember { mutableStateOf(false) }
                var currentTabIndex by remember { mutableIntStateOf(0) }
                val isDataLoading = isChannelLoading || isEpgLoading
                val extraDelay = getDynamicDelay()

                val repository = remember { SettingsRepository(context) }
                val mirakurunIp by repository.mirakurunIp.collectAsState(initial = "192.168.100.60")
                val mirakurunPort by repository.mirakurunPort.collectAsState(initial = "40772")
                val konomiIp by repository.konomiIp.collectAsState(initial = "https://192-168-100-60.local.konomi.tv")
                val konomiPort by repository.konomiPort.collectAsState(initial = "7000")

//                BackHandler(enabled = true) {
//                    when {
//                        isPlayerMode -> isPlayerMode = false
//                        isSettingsMode -> isSettingsMode = false
//                        else -> showExitDialog = true
//                    }
//                }
                // 再生中かどうかでBackHandlerの挙動を変える
                BackHandler(enabled = true) {
                    if (isPlayerMode) {
                        // 再生モードなら、終了ダイアログを出さずにリストに戻る
                        isPlayerMode = false
                    } else if (isSettingsMode) {
                        isSettingsMode = false
                    } else {
                        // ホーム画面なら終了ダイアログを表示
                        showExitDialog = true
                    }
                }

                if (isDataLoading) {
                    // 通信中は完全にLoadingを表示
                    LoadingScreen()
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            isPlayerMode -> {
                                selectedChannel?.let { currentChannel ->
                                    key(currentChannel.id) {
                                        LivePlayerScreen(
                                            channel = selectedChannel!!,
                                            groupedChannels = groupedChannels,
                                            mirakurunIp = mirakurunIp,
                                            mirakurunPort = mirakurunPort,
                                            isMiniListOpen = isMiniListOpen,
                                            onMiniListToggle = { isMiniListOpen = it },
                                            onChannelSelect = { selectedChannel = it },
                                            onBackPressed = { isPlayerMode = false } // ★ここを追加
                                        )
                                    }
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
                                    // ★ EpgViewModel を明示的に渡すか、HomeLauncherScreen内でHiltから取得する
                                    onChannelClick = { channel ->
                                        selectedChannel = channel
                                        currentTabIndex = 1
                                        isPlayerMode = true
                                    },
                                    onSettingsClick = { isSettingsMode = true },
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
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

    Log.i("getDynamicDelay", "Total RAM: $totalRamGb GB")


    return when {
        totalRamGb <= 1.5 -> 7000L // Fire TV Stickなど低スペック
        totalRamGb <= 3.0 -> 4500L // 標準的なTV
        else -> 1000L              // Shield TVなど高性能機
    }
}
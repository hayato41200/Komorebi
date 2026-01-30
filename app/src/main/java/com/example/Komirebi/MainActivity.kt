package com.example.Komirebi

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.*
import com.example.Komirebi.ui.theme.DTVClientTheme
import com.example.Komirebi.ui.theme.SettingsScreen
import com.example.Komirebi.data.SettingsRepository
import com.example.Komirebi.ui.components.ExitDialog
import com.example.Komirebi.ui.home.HomeLauncherScreen
import com.example.Komirebi.ui.home.LoadingScreen
import com.example.Komirebi.ui.live.LivePlayerScreen
import com.example.Komirebi.viewmodel.Channel
import com.example.Komirebi.viewmodel.ChannelViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // ← これに変える！
class MainActivity : ComponentActivity() {

    private val viewModel: ChannelViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_HDR
        }
        super.onCreate(savedInstanceState)
        viewModel.fetchChannels()

        setContent {
            DTVClientTheme {
                val context = LocalContext.current
                val activity = context as? Activity
                val isLoading by viewModel.isLoading.collectAsState()
                val groupedChannels by viewModel.groupedChannels.collectAsState()

                // --- 状態保持 ---
                var selectedChannel by remember { mutableStateOf<Channel?>(null) }
                var isPlayerMode by remember { mutableStateOf(false) }
                var isSettingsMode by remember { mutableStateOf(false) } // ★設定画面フラグ
                var isMiniListOpen by remember { mutableStateOf(false) }
                var showExitDialog by remember { mutableStateOf(false) }
                var currentTabIndex by remember { mutableIntStateOf(0) }
                val repository = remember { SettingsRepository(context) }
                val mirakurunIp by repository.mirakurunIp.collectAsState(initial = "192.168.100.60")
                val mirakurunPort by repository.mirakurunPort.collectAsState(initial = "40772")
                val konomiIp by repository.konomiIp.collectAsState(initial = "https://192-168-100-60.local.konomi.tv")
                val konomiPort by repository.konomiPort.collectAsState(initial = "7000")


                // システムバックボタン（リモコンの戻るキー）の制御
                BackHandler(enabled = true) {
                    when {
                        isPlayerMode -> isPlayerMode = false
                        isSettingsMode -> isSettingsMode = false
                        else -> showExitDialog = true
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingScreen()
                    }
                } else {
                    // --- 画面遷移の分岐 ---
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            // 1. プレイヤー画面
                            isPlayerMode -> {
                                selectedChannel?.let { currentChannel ->
                                    key(currentChannel.id) {
                                        LivePlayerScreen(
                                            channel = currentChannel,
                                            mirakurunIp = mirakurunIp,     // ★追加
                                            mirakurunPort = mirakurunPort, // ★追加
                                            groupedChannels = groupedChannels,
                                            isMiniListOpen = isMiniListOpen,
                                            onMiniListToggle = { isMiniListOpen = it },
                                            onChannelSelect = { selected ->
                                                selectedChannel = selected
                                                isMiniListOpen = false
                                                currentTabIndex = 1
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. 設定画面
                            isSettingsMode -> {
                                SettingsScreen(
                                    onBack = { isSettingsMode = false }
                                )
                            }

                            // 3. ホームランチャー画面
                            else -> {
                                HomeLauncherScreen(
                                    groupedChannels = groupedChannels,
                                    lastWatchedChannel = selectedChannel,
                                    mirakurunIp = mirakurunIp,
                                    mirakurunPort = mirakurunPort,
                                    konomiIp = konomiIp,
                                    konomiPort = konomiPort,
                                    initialTabIndex = currentTabIndex,
                                    onChannelClick = { channel ->
                                        selectedChannel = channel
                                        currentTabIndex = 1
                                        isPlayerMode = true
                                    },
                                    onSettingsClick = {
                                        isSettingsMode = true // ★設定画面へ遷移
                                    }
                                )
                            }
                        }
                    }
                }

                if (showExitDialog) {
                    ExitDialog(
                        onConfirm = { activity?.finish() },
                        onDismiss = { showExitDialog = false }
                    )
                }
            }
        }
    }
}
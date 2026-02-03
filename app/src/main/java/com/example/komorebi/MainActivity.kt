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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.*
import com.example.komorebi.ui.theme.KomorebiTheme
import com.example.komorebi.ui.theme.SettingsScreen
import com.example.komorebi.data.SettingsRepository
import com.example.komorebi.ui.components.ExitDialog
import com.example.komorebi.ui.home.HomeLauncherScreen
import com.example.komorebi.ui.home.LoadingScreen
import com.example.komorebi.ui.live.LivePlayerScreen
import com.example.komorebi.ui.main.MainRootScreen
import com.example.komorebi.viewmodel.Channel
import com.example.komorebi.viewmodel.ChannelViewModel
import com.example.komorebi.viewmodel.EpgViewModel
import com.example.komorebi.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val channelViewModel: ChannelViewModel by viewModels()
    private val epgViewModel: EpgViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初期データのロード
        channelViewModel.fetchChannels()
        epgViewModel.preloadAllEpgData()
        channelViewModel.fetchRecentRecordings()

        setContent {
            KomorebiTheme {
                var showExitDialog by remember { mutableStateOf(false) }

                // アプリのメインナビゲーション
                MainRootScreen(
                    channelViewModel = channelViewModel,
                    epgViewModel = epgViewModel,
                    homeViewModel = homeViewModel,
                    onExitApp = { showExitDialog = true }
                )

                if (showExitDialog) {
                    ExitDialog(
                        onConfirm = { finish() },
                        onDismiss = { showExitDialog = false }
                    )
                }
            }
        }
    }
}
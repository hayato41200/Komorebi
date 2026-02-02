package com.example.komorebi.ui.home

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.komorebi.data.model.EpgProgram
import com.example.komorebi.data.model.KonomiHistoryProgram
import com.example.komorebi.data.model.RecordedProgram
import com.example.komorebi.ui.program.EpgScreen
import com.example.komorebi.ui.video.VideoPlayerScreen
import com.example.komorebi.viewmodel.Channel
import com.example.komorebi.viewmodel.ChannelViewModel
import com.example.komorebi.viewmodel.EpgViewModel
import com.example.komorebi.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun HomeLauncherScreen(
    channelViewModel: ChannelViewModel = viewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    epgViewModel: EpgViewModel = hiltViewModel(),
    groupedChannels: Map<String, List<Channel>>,
    lastWatchedChannel: Channel?,
    mirakurunIp: String,
    mirakurunPort: String,
    konomiIp: String,
    konomiPort: String,
    onChannelClick: (Channel) -> Unit,
    initialTabIndex: Int = 0,
    onSettingsClick: () -> Unit = {},
    onUiReady: () -> Unit,
) {
    val tabs = listOf("ホーム", "ライブ", "番組表", "ビデオ")
    val scope = rememberCoroutineScope()

    // --- 状態管理 ---
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(if (lastWatchedChannel != null) 1 else initialTabIndex) }
    var focusedTabIndex by remember { mutableIntStateOf(selectedTabIndex) }
    var tabRowHasFocus by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedProgram by remember { mutableStateOf<RecordedProgram?>(null) }
    var epgSelectedProgram by remember { mutableStateOf<EpgProgram?>(null) }
    var lastFocusedChannelId by remember { mutableStateOf<String?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var selectedBroadcastingType by rememberSaveable { mutableStateOf("GR") }
    var isSuppressingTabChange by remember { mutableStateOf(false) }

    // --- フォーカス制御用 ---
    val tabFocusRequesters = remember { List(tabs.size) { FocusRequester() } }
    val contentFocusRequesters = remember { List(tabs.size) { FocusRequester() } }
    val epgTabFocusRequester = remember { FocusRequester() }
    val epgFirstCellFocusRequester = remember { FocusRequester() }
    var isEpgBroadcastingTabFocused by remember { mutableStateOf(false) }

    val activity = (LocalContext.current as? Activity)

    // --- データ購読 (Single Source of Truth) ---
    val recentRecordings by channelViewModel.recentRecordings.collectAsState()

    // ViewModel側でAPIとLocalDBが統合されたFlowを購読
    val watchHistory by homeViewModel.watchHistory.collectAsState()

    // ビデオタブ等の RecordedProgram 型を期待するコンポーネント用
    val watchHistoryPrograms = remember(watchHistory) {
        watchHistory.map { it.toRecordedProgram() }
    }

    // 描画安定化ロジック
    LaunchedEffect(Unit) {
        isVisible = true
        delay(300)
        onUiReady()
        delay(200)
        tabFocusRequesters[selectedTabIndex].requestFocus()
    }

    // バックハンドラー (ロジック維持)
    BackHandler(enabled = true) {
        when {
            epgSelectedProgram != null -> {
                isSuppressingTabChange = true
                epgSelectedProgram = null
                scope.launch {
                    delay(100)
                    epgFirstCellFocusRequester.requestFocus()
                    delay(400)
                    isSuppressingTabChange = false
                }
            }
            selectedProgram != null -> {
                selectedProgram = null
                homeViewModel.refreshHomeData()
            }
            selectedTabIndex == 2 -> {
                if (tabRowHasFocus) {
                    selectedTabIndex = 0
                    tabFocusRequesters[0].requestFocus()
                } else {
                    tabFocusRequesters[2].requestFocus()
                }
            }
            selectedTabIndex != 0 -> {
                selectedTabIndex = 0
                tabFocusRequesters[0].requestFocus()
            }
            else -> {
                showExitDialog = true
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("アプリを終了しますか？") },
            confirmButton = { Button(onClick = { activity?.finish() }) { Text("終了") } },
            dismissButton = { OutlinedButton(onClick = { showExitDialog = false }) { Text("キャンセル") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500))
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
                // --- ナビゲーションバー ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 40.dp, end = 40.dp)
                        .onFocusChanged { tabRowHasFocus = it.hasFocus },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            val isFocused = tabRowHasFocus && focusedTabIndex == index

                            Box(
                                modifier = Modifier
                                    .focusRequester(tabFocusRequesters[index])
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            focusedTabIndex = index
                                            if (!isSuppressingTabChange && epgSelectedProgram == null) {
                                                selectedTabIndex = index
                                            }
                                        }
                                    }
                                    .focusProperties {
                                        down = when (title) {
                                            "番組表" -> epgTabFocusRequester
                                            else -> contentFocusRequesters[index]
                                        }
                                    }
                                    .focusable()
                                    .clickable { selectedTabIndex = index }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = 1.2.sp,
                                            fontSize = 16.sp,
                                        ),
                                        color = if (isSelected) Color.White else if (isFocused) Color.White.copy(0.85f) else Color.White.copy(0.25f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.width(28.dp).height(1.5.dp).background(if (isSelected) Color.White else Color.Transparent))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "設定", tint = Color.White.copy(0.4f), modifier = Modifier.size(20.dp))
                    }
                }

                // --- コンテンツエリア ---
                Box(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                    if (selectedTabIndex == 0) {
                        HomeContents(
                            lastWatchedChannel = lastWatchedChannel,
                            watchHistory = watchHistory, // API/DB統合データを使用
                            onChannelClick = { onChannelClick(it) },
                            onHistoryClick = { history ->
                                selectedProgram = history.toRecordedProgram()
                            },
                            konomiIp = konomiIp,
                            konomiPort = konomiPort,
                            modifier = Modifier.focusRequester(contentFocusRequesters[0]),
                            externalFocusRequester = contentFocusRequesters[0]
                        )
                    }

                    if (selectedTabIndex == 1) {
                        LiveContent(
                            modifier = Modifier.focusRequester(contentFocusRequesters[1]),
                            groupedChannels = groupedChannels,
                            lastWatchedChannel = lastWatchedChannel,
                            lastFocusedChannelId = lastFocusedChannelId,
                            onFocusChannelChange = { lastFocusedChannelId = it },
                            mirakurunIp = mirakurunIp,
                            mirakurunPort = mirakurunPort,
                            topTabFocusRequester = tabFocusRequesters[1],
                            onChannelClick = onChannelClick,
                        )
                    }

                    if (selectedTabIndex == 2) {
                        EpgScreen(
                            viewModel = epgViewModel,
                            topTabFocusRequester = tabFocusRequesters[2],
                            tabFocusRequester = epgTabFocusRequester,
                            onBroadcastingTabFocusChanged = { isEpgBroadcastingTabFocused = it },
                            firstCellFocusRequester = epgFirstCellFocusRequester,
                            selectedProgram = epgSelectedProgram,
                            onProgramSelected = { epgSelectedProgram = it },
                            selectedBroadcastingType = selectedBroadcastingType,
                            onTypeSelected = { selectedBroadcastingType = it },
                            onChannelSelected = { channelId ->
                                val targetChannel = groupedChannels.values.flatten().find { it.id == channelId }
                                if (targetChannel != null) onChannelClick(targetChannel)
                            }
                        )
                    }

                    if (selectedTabIndex == 3) {
                        VideoTabContent(
                            recentRecordings = recentRecordings,
                            watchHistory = watchHistoryPrograms,
                            konomiIp = konomiIp,
                            konomiPort = konomiPort,
                            externalFocusRequester = contentFocusRequesters[3],
                            onProgramClick = { selectedProgram = it }
                        )
                    }
                }
            }
        }

        // プレイヤー表示
        if (selectedProgram != null) {
            // 保存処理
            LaunchedEffect(selectedProgram!!.id) {
                homeViewModel.saveToHistory(selectedProgram!!)
            }
            VideoPlayerScreen(
                program = selectedProgram!!,
                konomiIp = konomiIp,
                konomiPort = konomiPort,
                onBackPressed = {
                    selectedProgram = null
                    homeViewModel.refreshHomeData() // 視聴後に履歴を最新化
                }
            )
        }
    }
}
package com.example.komorebi.ui.home

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
@OptIn(ExperimentalTvMaterial3Api::class)
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
    onTabChange: (Int) -> Unit, // ★ MainActivityの状態と同期するために追加
    initialTabIndex: Int = 0,
    onUiReady: () -> Unit,
) {
    val tabs = listOf("ホーム", "ライブ", "番組表", "ビデオ")
    val scope = rememberCoroutineScope()

    // --- 状態管理 ---
    // ★ 修正：lastWatchedChannel による強制上書きを廃止。MainActivity から渡される initialTabIndex を尊重。
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTabIndex) }

    var tabRowHasFocus by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedProgram by remember { mutableStateOf<RecordedProgram?>(null) }
    var epgSelectedProgram by remember { mutableStateOf<EpgProgram?>(null) }
    var lastFocusedChannelId by remember { mutableStateOf<String?>(null) }
    var selectedBroadcastingType by rememberSaveable { mutableStateOf("GR") }
    var isSuppressingTabChange by remember { mutableStateOf(false) }

    // --- フォーカス制御 ---
    val tabFocusRequesters = remember { List(tabs.size) { FocusRequester() } }
    val contentFirstItemRequesters = remember { List(tabs.size) { FocusRequester() } }
    val epgTabFocusRequester = remember { FocusRequester() }
    val epgFirstCellFocusRequester = remember { FocusRequester() }

    val activity = (LocalContext.current as? Activity)

    // --- データ購読 ---
    val recentRecordings by channelViewModel.recentRecordings.collectAsState()
    val watchHistory by homeViewModel.watchHistory.collectAsState()
    val watchHistoryPrograms = remember(watchHistory) { watchHistory.map { it.toRecordedProgram() } }

    LaunchedEffect(Unit) {
        isVisible = true
        onUiReady()
        delay(300)
        // 戻ってきた時に適切なタブにフォーカスを当てる
        tabFocusRequesters[selectedTabIndex].requestFocus()
    }

    // ★ 戻るキーの挙動
    BackHandler(enabled = epgSelectedProgram != null || selectedProgram != null || !tabRowHasFocus) {
        when {
            epgSelectedProgram != null -> {
                epgSelectedProgram = null
                scope.launch {
                    delay(300)
                    epgFirstCellFocusRequester.requestFocus()
                }
            }
            selectedProgram != null -> {
                selectedProgram = null
            }
            !tabRowHasFocus -> {
                tabFocusRequesters[selectedTabIndex].requestFocus()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // --- ナビゲーションバー ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, start = 40.dp, end = 40.dp)
                .onFocusChanged { tabRowHasFocus = it.hasFocus }
                .focusGroup(),
            indicator = { tabPositions, doesTabRowHaveFocus ->
                TabRowDefaults.UnderlinedIndicator(
                    currentTabPosition = tabPositions[selectedTabIndex],
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.height(2.dp)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onFocus = {
                        if (!isSuppressingTabChange && epgSelectedProgram == null) {
                            selectedTabIndex = index
                            onTabChange(index) // ★ MainActivity側にも通知
                        }
                    },
                    modifier = Modifier
                        .focusRequester(tabFocusRequesters[index])
                        .focusProperties {
                            down = if (title == "番組表") epgTabFocusRequester else contentFirstItemRequesters[index]
                        }
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        ),
                        color = if (tabRowHasFocus || selectedTabIndex == index) Color.White else Color.White.copy(0.4f)
                    )
                }
            }
        }

        // --- コンテンツエリア ---
        Box(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
            when (selectedTabIndex) {
                0 -> HomeContents(
                    lastWatchedChannel = lastWatchedChannel,
                    watchHistory = watchHistory,
                    onChannelClick = onChannelClick,
                    onHistoryClick = { history -> selectedProgram = history.toRecordedProgram() },
                    konomiIp = konomiIp, konomiPort = konomiPort,
                    externalFocusRequester = contentFirstItemRequesters[0],
                    tabFocusRequester = FocusRequester()
                )
                1 -> LiveContent(
                    groupedChannels = groupedChannels,
                    lastWatchedChannel = lastWatchedChannel,
                    lastFocusedChannelId = lastFocusedChannelId,
                    onFocusChannelChange = { lastFocusedChannelId = it },
                    mirakurunIp = mirakurunIp, mirakurunPort = mirakurunPort,
                    onChannelClick = onChannelClick,
                    externalFocusRequester = contentFirstItemRequesters[1]
                )
                2 -> EpgScreen(
                    viewModel = epgViewModel,
                    topTabFocusRequester = FocusRequester(),
                    tabFocusRequester = epgTabFocusRequester,
                    firstCellFocusRequester = epgFirstCellFocusRequester,
                    selectedProgram = epgSelectedProgram,
                    onProgramSelected = { epgSelectedProgram = it },
                    selectedBroadcastingType = selectedBroadcastingType,
                    onTypeSelected = { selectedBroadcastingType = it },
                    onChannelSelected = { channelId ->
                        epgSelectedProgram = null
                        val targetChannel = groupedChannels.values.flatten().find { it.id == channelId }
                        if (targetChannel != null) onChannelClick(targetChannel)
                    }
                )
                3 -> VideoTabContent(
                    recentRecordings = recentRecordings,
                    watchHistory = watchHistoryPrograms,
                    konomiIp = konomiIp, konomiPort = konomiPort,
                    externalFocusRequester = contentFirstItemRequesters[3],
                    onProgramClick = { selectedProgram = it }
                )
            }
        }
    }

    if (selectedProgram != null) {
        LaunchedEffect(selectedProgram!!.id) {
            homeViewModel.saveToHistory(selectedProgram!!)
        }
        VideoPlayerScreen(
            program = selectedProgram!!,
            konomiIp = konomiIp, konomiPort = konomiPort,
            onBackPressed = {
                selectedProgram = null
                homeViewModel.refreshHomeData()
            }
        )
    }
}
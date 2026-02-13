package com.beeregg2001.komorebi.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.beeregg2001.komorebi.data.SettingsRepository
import com.beeregg2001.komorebi.ui.settings.OpenSourceLicensesScreen
import com.beeregg2001.komorebi.viewmodel.SettingsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val konomiIp by settingsViewModel.konomiIp.collectAsState()
    val konomiPort by settingsViewModel.konomiPort.collectAsState()
    val mirakurunIp by settingsViewModel.mirakurunIp.collectAsState()
    val mirakurunPort by settingsViewModel.mirakurunPort.collectAsState()
    val themePreset by settingsViewModel.themePreset.collectAsState()
    val themeCustomAccent by settingsViewModel.themeCustomAccent.collectAsState()
    val jikkyoEnabled by settingsViewModel.enableJikkyoOverlay.collectAsState()
    val jikkyoDensity by settingsViewModel.jikkyoDensity.collectAsState()
    val jikkyoOpacity by settingsViewModel.jikkyoOpacity.collectAsState()
    val jikkyoPosition by settingsViewModel.jikkyoPosition.collectAsState()
    val externalLinkageEnabled by settingsViewModel.enableExternalLinkage.collectAsState()
    val currentUser by settingsViewModel.currentUser.collectAsState()
    val isJikkyoSupported by settingsViewModel.isJikkyoSupported.collectAsState()
    val isLinkageSupported by settingsViewModel.isLinkageSupported.collectAsState()

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var editingItem by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showLicenses by remember { mutableStateOf(false) }

    val categories = listOf(
        Category("接続設定", Icons.Default.CastConnected),
        Category("連携設定", Icons.Default.Link),
        Category("表示設定", Icons.Default.Tv),
        Category("テーマ", Icons.Default.Palette),
        Category("アプリ情報", Icons.Default.Info)
    )

    if (showLicenses) {
        OpenSourceLicensesScreen(onBack = { showLicenses = false })
        return
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF111111))) {
        Column(
            modifier = Modifier.width(280.dp).fillMaxHeight().background(Color(0xFF0A0A0A)).padding(vertical = 48.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text("設定", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            categories.forEachIndexed { i, c ->
                CategoryItem(title = c.name, icon = c.icon, isSelected = selectedCategoryIndex == i, onFocused = { selectedCategoryIndex = i }, onClick = { selectedCategoryIndex = i })
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("戻る") }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (selectedCategoryIndex) {
                0 -> {
                    SettingsTitle("接続設定")
                    EditableSetting("KonomiTV アドレス", konomiIp) { editingItem = "KonomiTV アドレス" to konomiIp }
                    EditableSetting("KonomiTV ポート番号", konomiPort) { editingItem = "KonomiTV ポート番号" to konomiPort }
                    EditableSetting("Mirakurun IPアドレス", mirakurunIp) { editingItem = "Mirakurun IPアドレス" to mirakurunIp }
                    EditableSetting("Mirakurun ポート番号", mirakurunPort) { editingItem = "Mirakurun ポート番号" to mirakurunPort }
                }
                1 -> {
                    SettingsTitle("連携設定")
                    Text("ログイン状態: ${if (currentUser?.is_logged_in == true) "ログイン済み" else "未ログイン"}", color = Color.White)
                    Text("ユーザー: ${currentUser?.niconico_user_name ?: currentUser?.name ?: "-"}", color = Color.White.copy(alpha = 0.8f))
                    ToggleSetting("外部連携を有効化", externalLinkageEnabled && isLinkageSupported, enabled = isLinkageSupported) {
                        settingsViewModel.setExternalLinkageEnabled(it)
                    }
                    if (!isLinkageSupported) Text("※ サーバー capability で連携が未対応のため無効です", color = Color.Gray)
                }
                2 -> {
                    SettingsTitle("表示設定")
                    ToggleSetting("実況オーバーレイ", jikkyoEnabled && isJikkyoSupported, enabled = isJikkyoSupported) {
                        settingsViewModel.setJikkyoEnabled(it)
                    }
                    ChoiceSetting("表示密度", listOf("低", "中", "高"), (jikkyoDensity - 1).coerceIn(0, 2), enabled = isJikkyoSupported) {
                        settingsViewModel.setJikkyoDensity(it + 1)
                    }
                    ChoiceSetting("透過率", listOf("30%", "50%", "65%", "80%", "100%"), when {
                        jikkyoOpacity < 0.4f -> 0
                        jikkyoOpacity < 0.57f -> 1
                        jikkyoOpacity < 0.73f -> 2
                        jikkyoOpacity < 0.9f -> 3
                        else -> 4
                    }, enabled = isJikkyoSupported) {
                        val v = listOf(0.3f, 0.5f, 0.65f, 0.8f, 1f)[it]
                        settingsViewModel.setJikkyoOpacity(v)
                    }
                    ChoiceSetting("表示位置", listOf("Top", "Center", "Bottom"), listOf("Top", "Center", "Bottom").indexOf(jikkyoPosition).coerceAtLeast(0), enabled = isJikkyoSupported) {
                        settingsViewModel.setJikkyoPosition(listOf("Top", "Center", "Bottom")[it])
                    }
                    if (!isJikkyoSupported) Text("※ サーバー capability で実況未対応のため無効です", color = Color.Gray)
                }
                3 -> {
                    SettingsTitle("テーマ")
                    ChoiceSetting("プリセット", listOf("Dark", "Light", "HighContrast", "Custom"), listOf("Dark", "Light", "HighContrast", "Custom").indexOf(themePreset).coerceAtLeast(0)) {
                        settingsViewModel.setThemePreset(listOf("Dark", "Light", "HighContrast", "Custom")[it])
                    }
                    if (themePreset == "Custom") {
                        EditableSetting("カスタムアクセント", themeCustomAccent) { editingItem = "テーマ カラー" to themeCustomAccent }
                    }
                }
                4 -> {
                    SettingsTitle("アプリ情報")
                    Text("Komorebi", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Button(onClick = { showLicenses = true }) { Text("オープンソースライセンス") }
                }
            }
        }
    }

    editingItem?.let { (title, value) ->
        com.beeregg2001.komorebi.ui.components.InputDialog(
            title = title,
            initialValue = value,
            onDismiss = { editingItem = null },
            onConfirm = { newValue ->
                when (title) {
                    "KonomiTV アドレス" -> settingsViewModel.saveString(SettingsRepository.KONOMI_IP, newValue)
                    "KonomiTV ポート番号" -> settingsViewModel.saveString(SettingsRepository.KONOMI_PORT, newValue)
                    "Mirakurun IPアドレス" -> settingsViewModel.saveString(SettingsRepository.MIRAKURUN_IP, newValue)
                    "Mirakurun ポート番号" -> settingsViewModel.saveString(SettingsRepository.MIRAKURUN_PORT, newValue)
                    "テーマ カラー" -> settingsViewModel.setThemeAccent(newValue)
                }
                editingItem = null
            }
        )
    }
}

data class Category(val name: String, val icon: ImageVector)

@Composable private fun SettingsTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable private fun EditableSetting(title: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Text(value)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable private fun ToggleSetting(title: String, checked: Boolean, enabled: Boolean = true, onToggle: (Boolean) -> Unit) {
    Surface(onClick = { if (enabled) onToggle(!checked) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title)
            Text(if (checked) "ON" else "OFF")
        }
    }
}

@Composable private fun ChoiceSetting(title: String, options: List<String>, selectedIndex: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEachIndexed { i, text ->
                val selected = i == selectedIndex
                Surface(onClick = { if (enabled) onSelect(i) }, enabled = enabled) {
                    Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = if (selected) Color.Black else Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(selected = isSelected, onClick = onClick, modifier = modifier.fillMaxWidth(), tonalElevation = if (isSelected) 4.dp else 0.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Spacer(Modifier.width(12.dp))
            Text(title)
        }
    }
    if (isSelected) onFocused()
}

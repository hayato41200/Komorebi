package com.example.komorebi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.komorebi.data.model.KonomiHistoryProgram
import com.example.komorebi.data.repository.KonomiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // ★これを追加
class HomeViewModel @Inject constructor( // ★@Inject を追加
    private val repository: KonomiRepository
) : ViewModel() {

    // 視聴履歴のライブデータ
    private val _watchHistory = MutableStateFlow<List<KonomiHistoryProgram>>(emptyList())
    val watchHistory: StateFlow<List<KonomiHistoryProgram>> = _watchHistory.asStateFlow()

    // 読み込み状態の管理
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 初回起動時にデータを取得
        refreshHomeData()
    }

    /**
     * サーバーから最新の履歴とユーザー設定を取得する
     */
    fun refreshHomeData() {
        viewModelScope.launch {
            _isLoading.value = true

            // 履歴の取得
            val result = repository.getWatchHistory()
            result.onSuccess { history ->
                _watchHistory.value = history
            }.onFailure {
                // 必要に応じてエラーログ
            }

            // ユーザー設定（ピン留め等）も更新しておく
            repository.refreshUser()

            _isLoading.value = false
        }
    }
}
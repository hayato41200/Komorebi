package com.example.Komorebi.data.repository

import com.example.Komorebi.data.model.HistoryUpdateRequest
import com.example.Komorebi.data.model.KonomiHistoryProgram
import com.example.Komorebi.data.model.KonomiProgram
import com.example.Komorebi.data.model.KonomiUser
import com.example.Komorebi.viewmodel.KonomiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class KonomiRepository @Inject constructor(private val apiService: KonomiApi) {

    // ユーザー設定をキャッシュして、どこからでも素早く参照できるようにする
    private val _currentUser = MutableStateFlow<KonomiUser?>(null)
    val currentUser: StateFlow<KonomiUser?> = _currentUser.asStateFlow()

    /**
     * ユーザー情報（ピン留めリスト等）を更新する
     */
    suspend fun refreshUser() {
        try {
            val user = apiService.getCurrentUser()
            _currentUser.value = user
        } catch (e: Exception) {
            // エラーハンドリング（ログ出力など）
        }
    }

    /**
     * 視聴履歴を取得する
     */
    suspend fun getWatchHistory(): Result<List<KonomiHistoryProgram>> {
        return runCatching { apiService.getWatchHistory() }
    }

    /**
     * マイリストを取得する
     */
    suspend fun getBookmarks(): Result<List<KonomiProgram>> {
        return runCatching { apiService.getBookmarks() }
    }

    /**
     * 視聴位置を同期する
     */
    suspend fun syncPlaybackPosition(programId: String, position: Double) {
        try {
            apiService.updateWatchHistory(HistoryUpdateRequest(programId, position))
        } catch (e: Exception) {
            // 同期失敗しても視聴は継続させるため、ログのみ
        }
    }
}
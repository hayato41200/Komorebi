package com.beeregg2001.komorebi.data.repository

import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.beeregg2001.komorebi.data.api.KonomiApi
import com.beeregg2001.komorebi.data.local.dao.LastChannelDao
import com.beeregg2001.komorebi.data.local.dao.WatchHistoryDao
import com.beeregg2001.komorebi.data.local.entity.LastChannelEntity
import com.beeregg2001.komorebi.data.local.entity.WatchHistoryEntity
import com.beeregg2001.komorebi.data.model.HistoryUpdateRequest
import com.beeregg2001.komorebi.data.model.KonomiHistoryProgram
import com.beeregg2001.komorebi.data.model.KonomiProgram
import com.beeregg2001.komorebi.data.model.KonomiUser
import com.beeregg2001.komorebi.viewmodel.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KonomiRepository @Inject constructor(
    private val apiService: KonomiApi,
    private val watchHistoryDao: WatchHistoryDao,
    private val lastChannelDao: LastChannelDao
) {
    private val _currentUser = MutableStateFlow<KonomiUser?>(null)
    val currentUser: StateFlow<KonomiUser?> = _currentUser.asStateFlow()

    private val _isUserLoading = MutableStateFlow(false)
    val isUserLoading: StateFlow<Boolean> = _isUserLoading.asStateFlow()

    private val _userError = MutableStateFlow<String?>(null)
    val userError: StateFlow<String?> = _userError.asStateFlow()

    suspend fun refreshUser() {
        _isUserLoading.value = true
        runCatching { apiService.getCurrentUser() }
            .onSuccess {
                _currentUser.value = it
                _userError.value = null
            }
            .onFailure {
                _userError.value = it.message ?: "ユーザー情報の取得に失敗しました"
            }
        _isUserLoading.value = false
    }

    suspend fun getChannels() = apiService.getChannels()
    suspend fun getRecordedPrograms(page: Int = 1) = apiService.getRecordedPrograms(page = page)
    suspend fun searchRecordedPrograms(keyword: String, page: Int = 1) =
        apiService.searchVideos(keyword = keyword, page = page)

    suspend fun getBookmarks(): Result<List<KonomiProgram>> = runCatching { apiService.getBookmarks() }
    suspend fun getWatchHistory(): Result<List<KonomiHistoryProgram>> = runCatching { apiService.getWatchHistory() }

    fun getLocalWatchHistory() = watchHistoryDao.getAllHistory()

    suspend fun saveToLocalHistory(entity: WatchHistoryEntity) {
        watchHistoryDao.insertOrUpdate(entity)
    }

    fun getLastChannels() = lastChannelDao.getLastChannels()

    @OptIn(UnstableApi::class)
    suspend fun saveLastChannel(entity: LastChannelEntity) {
        lastChannelDao.insertOrUpdate(entity)
        Log.d("DEBUG", "Channel saved: ${entity.name}")
    }

    suspend fun syncPlaybackPosition(programId: String, position: Double) {
        runCatching { apiService.updateWatchHistory(HistoryUpdateRequest(programId, position)) }
    }

    fun buildStreamId(channel: Channel): String {
        val networkIdPart = when (channel.type) {
            "GR" -> channel.networkId.toString()
            "BS", "CS", "SKY", "BS4K" -> "${channel.networkId}00"
            else -> channel.networkId.toString()
        }
        return "${networkIdPart}${channel.serviceId}"
    }
}

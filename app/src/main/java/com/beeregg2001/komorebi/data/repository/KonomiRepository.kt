package com.beeregg2001.komorebi.data.repository

import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.beeregg2001.komorebi.data.api.KonomiApi
import com.beeregg2001.komorebi.data.local.dao.LastChannelDao
import com.beeregg2001.komorebi.data.local.dao.WatchHistoryDao
import com.beeregg2001.komorebi.data.local.entity.LastChannelEntity
import com.beeregg2001.komorebi.data.local.entity.WatchHistoryEntity
import com.beeregg2001.komorebi.data.model.Capability
import com.beeregg2001.komorebi.data.model.HistoryUpdateRequest
import com.beeregg2001.komorebi.data.model.KonomiHistoryProgram
import com.beeregg2001.komorebi.data.model.KonomiProgram
import com.beeregg2001.komorebi.data.model.KonomiUser
import com.beeregg2001.komorebi.viewmodel.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KonomiRepository @Inject constructor(
    private val apiService: KonomiApi,
    private val watchHistoryDao: WatchHistoryDao,
    private val lastChannelDao: LastChannelDao
) {
    companion object {
        const val UNSUPPORTED_MESSAGE = "機能未対応"
    }

    data class ApiGroup(val category: String, val apis: List<String>)

    val availableApis: List<ApiGroup> = listOf(
        ApiGroup("ライブ系", listOf("GET /api/channels", "GET /api/jikkyo/channels (probe)")),
        ApiGroup("録画系", listOf("GET /api/videos", "GET /api/videos/search", "GET/POST /api/programs/history", "GET/POST/DELETE /api/programs/bookmarks")),
        ApiGroup("予約系", listOf("GET /api/recording/reservations (probe)")),
        ApiGroup("ユーザー系", listOf("GET /api/users/me", "GET /api/settings/client (probe)")),
    )

    private val _currentUser = MutableStateFlow<KonomiUser?>(null)
    val currentUser: StateFlow<KonomiUser?> = _currentUser.asStateFlow()

    private val _capability = MutableStateFlow(Capability())
    val capability: StateFlow<Capability> = _capability.asStateFlow()

    suspend fun refreshCapability() {
        val supportsReservation = checkCapability { apiService.getReservationsProbe() }
        val supportsJikkyo = checkCapability { apiService.getJikkyoProbe() }
        val supportsQualityProfiles = checkCapability { apiService.getClientSettingsProbe() }
        _capability.value = Capability(
            supportsReservation = supportsReservation,
            supportsJikkyo = supportsJikkyo,
            supportsQualityProfiles = supportsQualityProfiles,
        )
    }

    private suspend fun checkCapability(call: suspend () -> retrofit2.Response<Unit>): Boolean {
        return runCatching { call() }
            .map { response -> response.isSuccessful }
            .getOrElse { false }
    }

    private fun mapRepositoryError(throwable: Throwable): Throwable {
        return when (throwable) {
            is HttpException -> if (throwable.code() == 404) UnsupportedOperationException(UNSUPPORTED_MESSAGE, throwable) else throwable
            is IOException -> throwable
            is UnsupportedOperationException -> throwable
            else -> throwable
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
        return runCatching { block() }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(mapRepositoryError(it)) }
        )
    }

    suspend fun refreshUser() {
        runCatching { apiService.getCurrentUser() }
            .onSuccess { _currentUser.value = it }
    }

    suspend fun getChannels() = apiService.getChannels()
    suspend fun getRecordedPrograms(page: Int = 1) = apiService.getRecordedPrograms(page = page)

    suspend fun searchRecordedPrograms(keyword: String, page: Int = 1) =
        apiService.searchVideos(keyword = keyword, page = page)

    suspend fun getBookmarks(): Result<List<KonomiProgram>> = safeApiCall { apiService.getBookmarks() }

    suspend fun getWatchHistory(): Result<List<KonomiHistoryProgram>> = safeApiCall { apiService.getWatchHistory() }

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
        safeApiCall { apiService.updateWatchHistory(HistoryUpdateRequest(programId, position)) }
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

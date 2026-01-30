package com.example.Komirebi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.Komirebi.data.model.RecordedApiResponse
import com.example.Komirebi.data.model.RecordedProgram
import com.example.Komirebi.data.SettingsRepository
import com.example.Komirebi.data.model.HistoryUpdateRequest
import com.example.Komirebi.data.model.KonomiHistoryProgram
import com.example.Komirebi.data.model.KonomiProgram
import com.example.Komirebi.data.model.KonomiUser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Path


// --- APIインターフェース ---
interface KonomiApi {
    @GET("api/channels")
    suspend fun getChannels(): ChannelApiResponse

    @GET("api/videos")
    suspend fun getRecordedPrograms(
        @Query("order") sort: String = "desc",
        @Query("page") page: Int = 1,
    ): RecordedApiResponse

    // --- ユーザー設定（ピン留めチャンネル等） ---
    @GET("api/users/me")
    suspend fun getCurrentUser(): KonomiUser

    // --- 視聴履歴 ---
    @GET("api/programs/history")
    suspend fun getWatchHistory(): List<KonomiHistoryProgram>

    // 視聴位置の更新（30秒以上視聴時などに叩く）
    @POST("api/programs/history")
    suspend fun updateWatchHistory(@Body request: HistoryUpdateRequest)

    // --- マイリスト（ブックマーク） ---
    @GET("api/programs/bookmarks")
    suspend fun getBookmarks(): List<KonomiProgram>

    @POST("api/programs/bookmarks/{program_id}")
    suspend fun addBookmark(@Path("program_id") programId: String)

    @DELETE("api/programs/bookmarks/{program_id}")
    suspend fun removeBookmark(@Path("program_id") programId: String)
}

// --- ViewModel ---
class ChannelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _groupedChannels = MutableStateFlow<Map<String, List<Channel>>>(emptyMap())
    val groupedChannels: StateFlow<Map<String, List<Channel>>> = _groupedChannels

    private val konomiIp = repository.konomiIp
    private val konomiPort = repository.konomiPort
    private var pollingJob: Job? = null

    // ビデオタブ用：新着録画リスト
    private val _recentRecordings = MutableStateFlow<List<RecordedProgram>>(emptyList())
    val recentRecordings: StateFlow<List<RecordedProgram>> = _recentRecordings

    // ビデオタブ用：ロード状態
    private val _isRecordingLoading = MutableStateFlow(false)
    val isRecordingLoading: StateFlow<Boolean> = _isRecordingLoading

    init {
        // ViewModelが作成されたらすぐに定期更新を開始する
        startPolling()
    }

    private suspend fun getApi(): KonomiApi {
        val ip = konomiIp.first()
        val port = konomiPort.first()
        val baseUrl = if (ip.startsWith("http")) "$ip:$port/" else "http://$ip:$port/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KonomiApi::class.java)
    }

    // 定期更新の開始
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchChannelsInternal()
                delay(30_000L) // 30秒間隔
            }
        }
    }

    // 手動更新
    fun fetchChannels() {
        viewModelScope.launch { fetchChannelsInternal() }
    }

    private suspend fun fetchChannelsInternal() {
        try {
            val apiService = getApi()
            val response = apiService.getChannels()

            val processed = withContext(Dispatchers.Default) {
                val all = mutableListOf<Channel>()
                response.terrestrial?.let { all.addAll(it) }
                response.bs?.let { all.addAll(it) }
                response.cs?.let { all.addAll(it) }
                response.sky?.let { all.addAll(it) }
                response.bs4k?.let { all.addAll(it) }

                // 元のフィルタリングロジックを維持
                all.filter { it.isDisplay }.groupBy { it.type }
            }

            _groupedChannels.value = processed
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    /**
     * 新着録画を取得する
     */
    fun fetchRecentRecordings() {
        viewModelScope.launch {
            _isRecordingLoading.value = true
            try {
                val apiService = getApi()
                // 最新の30件を取得
                val response = apiService.getRecordedPrograms(sort = "desc", page = 1)
                _recentRecordings.value = response.recordedPrograms
            } catch (e: Exception) {
                e.printStackTrace()
                // エラー時は空リストを流すなどの処理
            } finally {
                _isRecordingLoading.value = false
            }
        }
    }
}

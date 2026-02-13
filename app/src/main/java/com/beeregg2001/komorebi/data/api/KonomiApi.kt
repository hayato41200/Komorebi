package com.beeregg2001.komorebi.data.api

import com.beeregg2001.komorebi.data.model.HistoryUpdateRequest
import com.beeregg2001.komorebi.data.model.KonomiHistoryProgram
import com.beeregg2001.komorebi.data.model.KonomiProgram
import com.beeregg2001.komorebi.data.model.KonomiUser
import com.beeregg2001.komorebi.data.model.RecordedApiResponse
import com.beeregg2001.komorebi.viewmodel.ChannelApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KonomiApi {
    // --- ライブ系 API ---
    @GET("api/channels")
    suspend fun getChannels(): ChannelApiResponse

    // --- 録画系 API ---
    @GET("api/videos")
    suspend fun getRecordedPrograms(
        @Query("order") sort: String = "desc",
        @Query("page") page: Int = 1,
    ): RecordedApiResponse

    @GET("api/videos/search")
    suspend fun searchVideos(
        @Query("keyword") keyword: String,
        @Query("order") sort: String = "desc",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): RecordedApiResponse

    @GET("api/programs/history")
    suspend fun getWatchHistory(): List<KonomiHistoryProgram>

    @POST("api/programs/history")
    suspend fun updateWatchHistory(@Body request: HistoryUpdateRequest)

    @GET("api/programs/bookmarks")
    suspend fun getBookmarks(): List<KonomiProgram>

    @POST("api/programs/bookmarks/{program_id}")
    suspend fun addBookmark(@Path("program_id") programId: String)

    @DELETE("api/programs/bookmarks/{program_id}")
    suspend fun removeBookmark(@Path("program_id") programId: String)

    // --- 予約系 API (Capability 判定用 Probe) ---
    @GET("api/recording/reservations")
    suspend fun getReservationsProbe(): Response<Unit>

    // --- ユーザー系 API ---
    @GET("api/users/me")
    suspend fun getCurrentUser(): KonomiUser

    // --- バージョン差分吸収のための Probe API ---
    @GET("api/jikkyo/channels")
    suspend fun getJikkyoProbe(): Response<Unit>

    @GET("api/settings/client")
    suspend fun getClientSettingsProbe(): Response<Unit>
}

package com.beeregg2001.komorebi.data.repository

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.beeregg2001.komorebi.data.api.KonomiApi
import com.beeregg2001.komorebi.data.local.dao.ReservationDao
import com.beeregg2001.komorebi.data.local.entity.ReservationEntity
import com.beeregg2001.komorebi.data.local.entity.ReservationErrorType
import com.beeregg2001.komorebi.data.local.entity.ReservationStatus
import com.beeregg2001.komorebi.data.model.StartRecordingRequest
import com.beeregg2001.komorebi.data.local.entity.buildChannelRecordingKey
import com.beeregg2001.komorebi.data.local.entity.buildProgramReservationKey
import com.beeregg2001.komorebi.data.model.EpgChannelResponse
import com.beeregg2001.komorebi.data.model.EpgChannelWrapper
import com.beeregg2001.komorebi.data.model.EpgProgram
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

interface KonomiTvApiService {
    @GET("api/programs/timetable")
    suspend fun getEpgPrograms(
        @Query("start_time") startTime: String? = null,
        @Query("end_time") endTime: String? = null,
        @Query("channel_type") channelType: String? = null,
        @Query("pinned_channel_ids") pinnedChannelIds: String? = null
    ): EpgChannelResponse
}

enum class TaskErrorType {
    TUNER_SHORTAGE,
    DUPLICATED,
    NETWORK,
    UNKNOWN,
}

data class TaskActionResult(
    val success: Boolean,
    val errorType: TaskErrorType? = null,
    val detail: String? = null
)

class EpgRepository @Inject constructor(
    private val apiService: KonomiTvApiService,
    private val konomiApi: KonomiApi,
    private val reservationDao: ReservationDao
) {
    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchEpgData(
        startTime: OffsetDateTime,
        endTime: OffsetDateTime,
        channelType: String? = null
    ): Result<List<EpgChannelWrapper>> {
        return try {
            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val response = apiService.getEpgPrograms(
                startTime = startTime.format(formatter),
                endTime = endTime.format(formatter),
                channelType = channelType
            )
            Result.success(response.channels)
        } catch (e: Exception) {
            Log.e("EPG", "Fetch Error: $startTime to $endTime", e)
            Result.failure(e)
        }
    }

    suspend fun fetchPinnedChannels(pinnedIds: List<String>): Result<List<EpgChannelWrapper>> {
        return try {
            val response = apiService.getEpgPrograms(pinnedChannelIds = pinnedIds.joinToString(","))
            Result.success(response.channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchNowAndNextPrograms(channelId: String): Result<List<EpgProgram>> {
        return try {
            val now = OffsetDateTime.now()
            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val response = apiService.getEpgPrograms(
                startTime = now.minusHours(1).format(formatter),
                endTime = now.plusHours(6).format(formatter),
                pinnedChannelIds = channelId
            )
            val programs = response.channels.firstOrNull()?.programs.orEmpty()
                .filter { program ->
                    runCatching { OffsetDateTime.parse(program.end_time).isAfter(now) }.getOrDefault(false)
                }
                .sortedBy { it.start_time }
                .take(2)
            Result.success(programs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

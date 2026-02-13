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
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
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

    fun observeProgramReservation(programId: String): Flow<ReservationEntity?> {
        return reservationDao.observeByKey(buildProgramReservationKey(programId))
    }

    fun observeChannelRecording(channelId: String): Flow<ReservationEntity?> {
        return reservationDao.observeByKey(buildChannelRecordingKey(channelId))
    }

    suspend fun toggleProgramReservation(
        programId: String,
        channelId: String,
        title: String,
        startAt: String?,
        endAt: String?
    ): TaskActionResult {
        val key = buildProgramReservationKey(programId)
        val current = reservationDao.getByKey(key)
        val shouldReserve = current?.isReserved != true

        saveState(
            ReservationEntity(
                key = key,
                targetType = "program",
                targetId = programId,
                channelId = channelId,
                title = title,
                startAt = startAt,
                endAt = endAt,
                isReserved = current?.isReserved == true,
                isRecording = current?.isRecording == true,
                status = ReservationStatus.LOADING.value
            )
        )

        return runCatching {
            if (shouldReserve) konomiApi.reserveProgram(programId) else konomiApi.cancelProgramReservation(programId)
        }.fold(
            onSuccess = {
                saveState(
                    ReservationEntity(
                        key = key,
                        targetType = "program",
                        targetId = programId,
                        channelId = channelId,
                        title = title,
                        startAt = startAt,
                        endAt = endAt,
                        isReserved = shouldReserve,
                        status = ReservationStatus.SUCCESS.value
                    )
                )
                TaskActionResult(success = true)
            },
            onFailure = { throwable ->
                val mapped = mapError(throwable)
                saveState(
                    ReservationEntity(
                        key = key,
                        targetType = "program",
                        targetId = programId,
                        channelId = channelId,
                        title = title,
                        startAt = startAt,
                        endAt = endAt,
                        isReserved = current?.isReserved == true,
                        status = ReservationStatus.ERROR.value,
                        errorType = mapped.errorType?.toReservationErrorType(),
                        errorDetail = mapped.detail
                    )
                )
                mapped
            }
        )
    }

    suspend fun startChannelRecording(channelId: String, channelName: String): TaskActionResult {
        val key = buildChannelRecordingKey(channelId)
        val current = reservationDao.getByKey(key)

        saveState(
            ReservationEntity(
                key = key,
                targetType = "channel",
                targetId = channelId,
                channelId = channelId,
                title = channelName,
                startAt = null,
                endAt = null,
                isRecording = current?.isRecording == true,
                status = ReservationStatus.LOADING.value
            )
        )

        return runCatching {
            konomiApi.startChannelRecording(StartRecordingRequest(channelId))
        }.fold(
            onSuccess = {
                saveState(
                    ReservationEntity(
                        key = key,
                        targetType = "channel",
                        targetId = channelId,
                        channelId = channelId,
                        title = channelName,
                        startAt = null,
                        endAt = null,
                        isRecording = true,
                        status = ReservationStatus.SUCCESS.value
                    )
                )
                TaskActionResult(success = true)
            },
            onFailure = { throwable ->
                val mapped = mapError(throwable)
                saveState(
                    ReservationEntity(
                        key = key,
                        targetType = "channel",
                        targetId = channelId,
                        channelId = channelId,
                        title = channelName,
                        startAt = null,
                        endAt = null,
                        isRecording = current?.isRecording == true,
                        status = ReservationStatus.ERROR.value,
                        errorType = mapped.errorType?.toReservationErrorType(),
                        errorDetail = mapped.detail
                    )
                )
                mapped
            }
        )
    }

    private suspend fun saveState(entity: ReservationEntity) {
        reservationDao.upsert(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun mapError(throwable: Throwable): TaskActionResult {
        if (throwable is IOException) {
            return TaskActionResult(false, TaskErrorType.NETWORK, "ネットワークに接続できません。")
        }
        if (throwable is HttpException) {
            val body = throwable.response()?.errorBody()?.string().orEmpty()
            val message = if (body.isNotBlank()) body else throwable.message()
            val lower = message.lowercase()
            return when {
                lower.contains("tuner") || message.contains("チューナー") || throwable.code() == 503 ->
                    TaskActionResult(false, TaskErrorType.TUNER_SHORTAGE, "チューナーが不足しています。")
                lower.contains("duplicate") || message.contains("重複") || throwable.code() == 409 ->
                    TaskActionResult(false, TaskErrorType.DUPLICATED, "同一時間帯の重複予約です。")
                else -> TaskActionResult(false, TaskErrorType.UNKNOWN, message.ifBlank { "予約/録画処理に失敗しました。" })
            }
        }
        return TaskActionResult(false, TaskErrorType.UNKNOWN, throwable.message ?: "予約/録画処理に失敗しました。")
    }
}

private fun TaskErrorType.toReservationErrorType(): String = when (this) {
    TaskErrorType.TUNER_SHORTAGE -> ReservationErrorType.TUNER_SHORTAGE.value
    TaskErrorType.DUPLICATED -> ReservationErrorType.DUPLICATED.value
    TaskErrorType.NETWORK -> ReservationErrorType.NETWORK.value
    TaskErrorType.UNKNOWN -> ReservationErrorType.UNKNOWN.value
}

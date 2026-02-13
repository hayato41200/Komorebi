package com.beeregg2001.komorebi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey
    val key: String,
    val targetType: String,
    val targetId: String,
    val channelId: String?,
    val title: String,
    val startAt: String?,
    val endAt: String?,
    val isReserved: Boolean = false,
    val isRecording: Boolean = false,
    val status: String = ReservationStatus.IDLE.value,
    val errorType: String? = null,
    val errorDetail: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReservationStatus(val value: String) {
    IDLE("idle"),
    LOADING("loading"),
    SUCCESS("success"),
    ERROR("error")
}

enum class ReservationErrorType(val value: String) {
    TUNER_SHORTAGE("tuner_shortage"),
    DUPLICATED("duplicated"),
    NETWORK("network"),
    UNKNOWN("unknown")
}

fun buildProgramReservationKey(programId: String): String = "program:$programId"
fun buildChannelRecordingKey(channelId: String): String = "channel:$channelId"

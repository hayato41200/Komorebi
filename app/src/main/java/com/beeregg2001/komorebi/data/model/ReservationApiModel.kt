package com.beeregg2001.komorebi.data.model

import com.google.gson.annotations.SerializedName

data class ReservationApiError(
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("error") val error: String? = null
)

data class StartRecordingRequest(
    @SerializedName("channel_id") val channelId: String
)

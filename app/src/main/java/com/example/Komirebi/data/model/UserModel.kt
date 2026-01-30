package com.example.Komirebi.data.model

data class KonomiUser(
    val id: Int,
    val name: String,
    val pinned_channel_ids: List<String>, // "32736-1024" 形式
    // 他のユーザー設定項目...
)

data class KonomiHistoryProgram(
    val program: KonomiProgram,
    val playback_position: Double, // 再生位置（秒）
    val last_watched_at: String    // ISO形式の文字列
)

data class KonomiProgram(
    val id: String,
    val title: String,
    val description: String,
    val start_time: String,
    val end_time: String,
    val channel_id: String,
    // ...あらすじやジャンルなど
)

data class HistoryUpdateRequest(
    val program_id: String,      // 番組ID
    val playback_position: Double // 再生位置（秒単位）
)
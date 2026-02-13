package com.beeregg2001.komorebi.ui.live

enum class AudioMode { MAIN, SUB }
enum class SubMenuCategory { AUDIO, VIDEO, SUBTITLE, QUALITY } // ★QUALITY追加
enum class StreamSource { MIRAKURUN, KONOMITV }

enum class ChannelKeyMode(val value: String, val label: String) {
    CHANNEL("channel", "左右キー: 選局"),
    DISABLED("disabled", "左右キー: 無効");

    companion object {
        fun fromValue(value: String): ChannelKeyMode =
            entries.find { it.value == value } ?: CHANNEL
    }
}

enum class LCropPreset(
    val label: String,
    val leftCropRatio: Float,
    val topCropRatio: Float
) {
    OFF("L字クロップ: OFF", 0f, 0f),
    WEAK("L字クロップ: 弱", 0.06f, 0.04f),
    STRONG("L字クロップ: 強", 0.12f, 0.08f)
}

data class LivePlaybackCommandState(
    val quality: StreamQuality,
    val cropPreset: LCropPreset,
    val channelKeyMode: ChannelKeyMode
)

// ★今回追加: 画質設定用のEnum
enum class StreamQuality(val value: String, val label: String) {
    Q1080P_60FPS("1080p-60fps", "1080p (60fps)"),
    Q1080P("1080p", "1080p"),
    Q720P("720p", "720p"),
    Q480P("480p", "480p"),
    Q360P("360p", "360p"),
    Q240P("240p", "240p");

    companion object {
        fun next(current: StreamQuality): StreamQuality {
            val values = entries.toTypedArray()
            return values[(current.ordinal + 1) % values.size]
        }
        fun fromValue(value: String): StreamQuality {
            return entries.find { it.value == value } ?: Q1080P_60FPS
        }
    }
}

object LivePlayerConstants {
    const val TAG_SUBTITLE = "SubtitleDebug"
    const val SUBTITLE_SYNC_OFFSET_MS = -500L
}

package com.beeregg2001.komorebi.ui.live

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class LivePlayerManager(private val player: ExoPlayer) {
    fun reconnectStream(streamUrl: String) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()
    }
}

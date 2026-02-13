package com.beeregg2001.komorebi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_channels")
data class PinnedChannelEntity(
    @PrimaryKey val channelId: String,
    val pinnedAt: Long = System.currentTimeMillis()
)

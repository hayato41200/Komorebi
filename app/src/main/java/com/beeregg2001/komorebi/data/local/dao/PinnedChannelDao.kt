package com.beeregg2001.komorebi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeregg2001.komorebi.data.local.entity.PinnedChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedChannelDao {
    @Query("SELECT * FROM pinned_channels ORDER BY pinnedAt ASC")
    fun getPinnedChannels(): Flow<List<PinnedChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PinnedChannelEntity)

    @Query("DELETE FROM pinned_channels WHERE channelId = :channelId")
    suspend fun delete(channelId: String)
}

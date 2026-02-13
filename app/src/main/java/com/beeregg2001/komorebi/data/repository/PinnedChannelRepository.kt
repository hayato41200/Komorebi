package com.beeregg2001.komorebi.data.repository

import com.beeregg2001.komorebi.data.local.dao.PinnedChannelDao
import com.beeregg2001.komorebi.data.local.entity.PinnedChannelEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinnedChannelRepository @Inject constructor(
    private val pinnedChannelDao: PinnedChannelDao
) {
    fun getPinnedChannels(): Flow<List<PinnedChannelEntity>> = pinnedChannelDao.getPinnedChannels()

    suspend fun pinChannel(channelId: String) = pinnedChannelDao.upsert(PinnedChannelEntity(channelId = channelId))

    suspend fun unpinChannel(channelId: String) = pinnedChannelDao.delete(channelId)
}

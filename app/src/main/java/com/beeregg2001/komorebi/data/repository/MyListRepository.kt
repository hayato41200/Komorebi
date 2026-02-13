package com.beeregg2001.komorebi.data.repository

import com.beeregg2001.komorebi.data.local.dao.MyListDao
import com.beeregg2001.komorebi.data.local.entity.MyListEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyListRepository @Inject constructor(
    private val myListDao: MyListDao
) {
    fun getMyList(): Flow<List<MyListEntity>> = myListDao.getMyList()

    suspend fun add(programId: Int) = myListDao.upsert(MyListEntity(programId = programId))

    suspend fun remove(programId: Int) = myListDao.delete(programId)
}

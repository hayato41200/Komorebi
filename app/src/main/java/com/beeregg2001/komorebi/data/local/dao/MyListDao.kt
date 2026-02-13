package com.beeregg2001.komorebi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeregg2001.komorebi.data.local.entity.MyListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyListDao {
    @Query("SELECT * FROM my_list ORDER BY addedAt DESC")
    fun getMyList(): Flow<List<MyListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MyListEntity)

    @Query("DELETE FROM my_list WHERE programId = :programId")
    suspend fun delete(programId: Int)
}

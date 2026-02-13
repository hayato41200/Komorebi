package com.beeregg2001.komorebi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeregg2001.komorebi.data.local.entity.ReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReservationEntity)

    @Query("SELECT * FROM reservations WHERE key = :key LIMIT 1")
    fun observeByKey(key: String): Flow<ReservationEntity?>

    @Query("SELECT * FROM reservations WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): ReservationEntity?
}

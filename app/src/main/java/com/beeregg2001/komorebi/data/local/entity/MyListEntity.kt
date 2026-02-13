package com.beeregg2001.komorebi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_list")
data class MyListEntity(
    @PrimaryKey val programId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

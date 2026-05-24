package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val translatedTitle: String,
    val synopsis: String,
    val by: String,
    val score: Int,
    val time: Long,
    val url: String?,
    val fetchedAt: Long = System.currentTimeMillis()
)

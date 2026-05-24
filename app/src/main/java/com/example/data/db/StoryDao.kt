package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY score DESC, fetchedAt DESC")
    fun getTopStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("DELETE FROM stories")
    suspend fun deleteAllStories()

    @Transaction
    suspend fun refreshStories(stories: List<StoryEntity>) {
        deleteAllStories()
        insertStories(stories)
    }
}

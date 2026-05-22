package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY timestamp DESC")
    fun getAllDecisions(): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntity): Long

    @Query("DELETE FROM decisions WHERE id = :id")
    suspend fun deleteDecisionById(id: Long)

    @Query("DELETE FROM decisions")
    suspend fun clearAll()
}

@Database(entities = [DecisionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun decisionDao(): DecisionDao
}

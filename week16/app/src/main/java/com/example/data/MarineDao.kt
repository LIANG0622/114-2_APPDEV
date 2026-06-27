package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MarineDao {
    @Query("SELECT * FROM marine_records ORDER BY time DESC")
    fun getAllRecords(): Flow<List<MarineRecord>>

    @Query("SELECT * FROM marine_records WHERE isUserNote = 0 ORDER BY time DESC")
    fun getOfficialObservations(): Flow<List<MarineRecord>>

    @Query("SELECT * FROM marine_records WHERE isUserNote = 1 ORDER BY time DESC")
    fun getUserNotes(): Flow<List<MarineRecord>>

    @Query("SELECT * FROM marine_records WHERE isFavorite = 1 ORDER BY time DESC")
    fun getFavoriteRecords(): Flow<List<MarineRecord>>

    @Query("SELECT * FROM marine_records WHERE month = :month AND isUserNote = 0 ORDER BY time DESC")
    fun getRecordsByMonth(month: Int): Flow<List<MarineRecord>>

    /**
     * 💡 歷史分頁專用：利用資料庫強大的篩選功能，完美解決 CSV 記憶體卡死問題
     */
    @Query("""
        SELECT * FROM marine_records 
        WHERE isUserNote = 0
          AND (:year = 0 OR year = :year)
          AND (:month = 0 OR month = :month)
          AND windSpeed >= :minWind
          AND waveHeight >= :minWave
          AND (time LIKE '%' || :query || '%' 
               OR windDirection LIKE '%' || :query || '%' 
               OR waveDirection LIKE '%' || :query || '%')
        ORDER BY time DESC
    """)
    fun getFilteredHistory(
        query: String,
        year: Int,
        month: Int,
        minWind: Double,
        minWave: Double
    ): Flow<List<MarineRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MarineRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MarineRecord>)

    @Update
    suspend fun updateRecord(record: MarineRecord)

    @Delete
    suspend fun deleteRecord(record: MarineRecord)

    @Query("SELECT COUNT(*) FROM marine_records WHERE isUserNote = 0")
    suspend fun getCount(): Int
}
package com.example.data

import kotlinx.coroutines.flow.Flow

class MarineRepository(private val marineDao: MarineDao) {

    val allRecords: Flow<List<MarineRecord>> = marineDao.getAllRecords()
    val officialObservations: Flow<List<MarineRecord>> = marineDao.getOfficialObservations()
    val userNotes: Flow<List<MarineRecord>> = marineDao.getUserNotes()
    val favoriteRecords: Flow<List<MarineRecord>> = marineDao.getFavoriteRecords()

    fun getRecordsByMonth(month: Int): Flow<List<MarineRecord>> {
        return marineDao.getRecordsByMonth(month)
    }

    /**
     * 💡 歷史分頁專用：呼叫資料庫做精準篩選
     */
    fun getFilteredHistory(
        query: String,
        year: Int,
        month: Int,
        minWind: Double,
        minWave: Double
    ): Flow<List<MarineRecord>> {
        return marineDao.getFilteredHistory(query, year, month, minWind, minWave)
    }

    suspend fun insert(record: MarineRecord) {
        marineDao.insertRecord(record)
    }

    suspend fun insertAll(records: List<MarineRecord>) {
        marineDao.insertAll(records)
    }

    suspend fun update(record: MarineRecord) {
        marineDao.updateRecord(record)
    }

    suspend fun delete(record: MarineRecord) {
        marineDao.deleteRecord(record)
    }

    suspend fun getCount(): Int {
        return marineDao.getCount()
    }
}
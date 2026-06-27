package com.example.data

import android.content.Context

object HistoryDataParser {
    @Volatile
    private var cachedRecords: List<MarineRecord>? = null

    /**
     * 使用標準雙重檢查鎖（Double-Checked Locking），確保執行緒安全與變數類型正確
     */
    fun getAllRecords(context: Context): List<MarineRecord> {
        // 第一層檢查：如果已經有快取，直接返回
        val currentCache = cachedRecords
        if (currentCache != null) {
            return currentCache
        }

        // 第二層檢查：加鎖同步讀取 CSV
        return synchronized(this) {
            val secondCache = cachedRecords
            if (secondCache != null) {
                secondCache
            } else {
                val records = try {
                    MarineDatabase.generateInitialRecordsFromCsv(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
                cachedRecords = records
                records
            }
        }
    }
}
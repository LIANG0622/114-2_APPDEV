package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marine_records")
data class MarineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: String,            // Date time: e.g. "2024-10-15 14:00"
    val year: Int,               // e.g. 2024, 2025
    val month: Int,              // 1 to 12
    val waveHeight: Double,      // 波高 (meters)
    val wavePeriod: Double,      // 週期 (seconds)
    val waveDirection: String,   // 波向 (e.g. "NNE")
    val windSpeed: Double,       // 風速 (m/s)
    val windDirection: String,   // 風向 (e.g. "NNE")
    val isFavorite: Boolean = false,
    val isUserNote: Boolean = false,
    val userNoteText: String? = null,
    val userPhotoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

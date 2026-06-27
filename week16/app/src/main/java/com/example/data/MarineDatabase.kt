package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sqrt

@Database(entities = [MarineRecord::class], version = 3, exportSchema = false)
abstract class MarineDatabase : RoomDatabase() {
    abstract fun marineDao(): MarineDao

    companion object {
        @Volatile
        private var INSTANCE: MarineDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MarineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarineDatabase::class.java,
                    "marine_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(MarineDatabaseCallback(context.applicationContext, scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun generateInitialRecordsFromCsv(context: Context): List<MarineRecord> {
            val records = mutableListOf<MarineRecord>()
            try {
                context.assets.open("marine_data.csv").bufferedReader().useLines { lines ->
                    var isFirst = true
                    lines.forEach { line ->
                        if (isFirst) {
                            isFirst = false // skip header
                            return@forEach
                        }
                        val parts = line.split(",")
                        if (parts.size >= 9) {
                            val year = parts[0].trim().toIntOrNull() ?: 2024
                            val month = parts[1].trim().toIntOrNull() ?: 1
                            val day = parts[2].trim().toIntOrNull() ?: 1
                            val hour = parts[3].trim().toIntOrNull() ?: 0
                            
                            val waveHeight = parts[4].trim().toDoubleOrNull() ?: 0.0
                            val wavePeriod = parts[5].trim().toDoubleOrNull() ?: 0.0
                            
                            val waveDirDeg = parts[6].trim().toDoubleOrNull() ?: -9.9
                            val waveDirString = if (waveDirDeg < 0) "N/A" else degreeToCompass(waveDirDeg)
                            
                            val windSpeed = parts[7].trim().toDoubleOrNull() ?: 0.0
                            
                            val windDirDeg = parts[8].trim().toDoubleOrNull() ?: -9.9
                            val windDirString = if (windDirDeg < 0) "N/A" else degreeToCompass(windDirDeg)
                            
                            if (waveHeight == -9.9 && windSpeed == -9.9) {
                                return@forEach
                            }
                            
                            val displayWaveHeight = if (waveHeight == -9.9) 0.0 else waveHeight
                            val displayWavePeriod = if (wavePeriod == -9.9) 0.0 else wavePeriod
                            val displayWindSpeed = if (windSpeed == -9.9) 0.0 else windSpeed
                            
                            val dateString = String.format("%04d-%02d-%02d %02d:00", year, month, day, hour)
                            
                            records.add(
                                MarineRecord(
                                    time = dateString,
                                    year = year,
                                    month = month,
                                    waveHeight = displayWaveHeight,
                                    wavePeriod = displayWavePeriod,
                                    waveDirection = waveDirString,
                                    windSpeed = displayWindSpeed,
                                    windDirection = windDirString,
                                    isFavorite = false,
                                    isUserNote = false
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (records.isEmpty()) {
                return generateInitialRecords()
            }
            return records
        }

        fun degreeToCompass(degree: Double): String {
            val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            val index = (((degree + 11.25) / 22.5).toInt()) % 16
            return directions[index.coerceIn(0, 15)]
        }

        fun generateInitialRecords(): List<MarineRecord> {
            val records = mutableListOf<MarineRecord>()
            val random = Random(42) // Fixed seed for reproducible statistics
            
            val years = listOf(2024, 2025)
            val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            
            for (year in years) {
                for (month in 1..12) {
                    val regression = RegressionConstants.getForMonth(month)

                    // Generate 40 records per month for rich density distribution
                    for (i in 1..40) {
                        val day = (i / 2) + 1
                        val hour = (i % 24)
                        val dateString = String.format("%04d-%02d-%02d %02d:00", year, month, day, hour)
                        
                        // Scale speed towards maxX of regression limits safely
                        val factor = random.nextDouble()
                        val speed = 1.0 + (factor * factor) * (regression.maxX - 4.5) + random.nextGaussian() * 0.3
                        
                        val baseWave = regression.a * speed * speed + regression.b * speed + regression.c
                        val stdDev = 0.42 * (1.15 - regression.r)
                        val noise = random.nextGaussian() * stdDev
                        val height = baseWave + noise
                        
                        // Period depends roughly on wave heights
                        val period = 4.0 + sqrt(height.coerceAtLeast(0.0)) * 1.2
                        
                        val windDir = if (month in listOf(10, 11, 12, 1, 2)) {
                            val list = listOf("N", "NNE", "NE")
                            list[random.nextInt(list.size)]
                        } else if (month in listOf(6, 7, 8)) {
                            val list = listOf("S", "SSW", "SW")
                            list[random.nextInt(list.size)]
                        } else {
                            directions[random.nextInt(directions.size)]
                        }
                        
                        val waveDir = if (month in listOf(10, 11, 12, 1, 2)) {
                            val list = listOf("NNE", "NE")
                            list[random.nextInt(list.size)]
                        } else if (month in listOf(6, 7, 8)) {
                            val list = listOf("SSW", "SW")
                            list[random.nextInt(list.size)]
                        } else {
                            directions[random.nextInt(directions.size)]
                        }

                        records.add(
                            MarineRecord(
                                time = dateString,
                                year = year,
                                month = month,
                                waveHeight = Math.max(0.1, Math.round(height * 10.0) / 10.0),
                                wavePeriod = Math.max(2.0, Math.round(period * 10.0) / 10.0),
                                waveDirection = waveDir,
                                windSpeed = Math.max(0.1, Math.round(speed * 10.0) / 10.0),
                                windDirection = windDir,
                                isFavorite = false,
                                isUserNote = false
                            )
                        )
                    }
                }
            }
            return records
        }
    }

    private class MarineDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    database.marineDao().insertAll(generateInitialRecordsFromCsv(context))
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    database.marineDao().insertAll(generateInitialRecordsFromCsv(context))
                }
            }
        }
    }
}

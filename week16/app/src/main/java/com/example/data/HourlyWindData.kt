package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.Color

object HourlyWindData {
    private var isLoaded = false
    private val monthlyHourlyAverages = mutableMapOf<Int, List<Double>>()

    // Fallback static averages in case asset reading fails
    private val fallbackAverages = mapOf(
        1 to listOf(9.8, 9.5, 9.9, 9.6, 9.4, 9.6, 9.6, 9.7, 9.7, 9.7, 9.9, 10.2, 10.3, 10.6, 11.0, 11.2, 11.3, 11.4, 11.0, 10.8, 10.5, 10.0, 9.8, 9.6),
        2 to listOf(8.9, 8.9, 9.1, 8.9, 8.9, 9.3, 9.3, 8.8, 8.9, 8.7, 9.4, 9.2, 9.2, 9.4, 9.3, 9.6, 9.8, 10.0, 9.9, 10.0, 9.7, 9.4, 9.6, 9.0),
        3 to listOf(8.3, 8.1, 7.9, 7.7, 8.3, 8.4, 8.2, 7.9, 7.9, 8.0, 8.2, 8.2, 8.8, 8.9, 8.9, 8.9, 9.1, 8.9, 9.2, 8.8, 8.5, 8.2, 7.7, 8.2),
        4 to listOf(5.8, 5.8, 5.3, 5.3, 5.2, 5.5, 5.1, 5.6, 5.4, 6.2, 6.4, 6.0, 6.3, 6.4, 6.7, 7.0, 6.5, 6.8, 6.9, 6.2, 5.5, 5.6, 5.7, 5.9),
        5 to listOf(5.4, 5.1, 5.4, 5.3, 5.8, 5.4, 5.4, 5.7, 5.7, 5.7, 5.8, 6.0, 6.3, 6.0, 6.1, 6.4, 6.5, 5.9, 5.9, 5.3, 6.1, 5.3, 5.5, 5.1),
        6 to listOf(4.9, 4.8, 5.1, 5.0, 4.8, 4.9, 4.9, 5.0, 5.0, 5.3, 5.1, 5.2, 5.8, 5.9, 6.1, 6.1, 5.8, 5.4, 5.5, 5.0, 5.3, 4.7, 4.5, 4.7),
        7 to listOf(5.1, 5.0, 5.1, 4.8, 4.8, 5.3, 5.0, 5.0, 5.2, 5.4, 5.3, 5.7, 6.1, 6.7, 6.5, 6.1, 6.1, 5.9, 5.7, 6.0, 5.5, 5.2, 4.9, 4.9),
        8 to listOf(4.9, 4.6, 4.6, 4.5, 4.7, 4.5, 4.3, 4.6, 4.2, 4.3, 4.4, 4.7, 4.9, 5.4, 5.3, 5.5, 5.6, 5.3, 4.9, 4.8, 4.5, 4.6, 4.6, 4.7),
        9 to listOf(3.6, 3.6, 3.5, 3.6, 3.7, 3.6, 3.7, 3.5, 3.3, 3.5, 3.9, 4.2, 4.8, 5.4, 5.1, 5.1, 5.1, 5.1, 5.1, 5.0, 4.5, 4.0, 3.8, 3.6),
        10 to listOf(9.0, 9.0, 8.7, 8.3, 9.2, 8.0, 8.5, 8.7, 8.7, 8.7, 9.0, 8.0, 9.1, 9.4, 10.4, 10.3, 10.8, 10.3, 10.2, 10.0, 10.3, 10.0, 10.0, 8.8),
        11 to listOf(10.9, 11.3, 10.6, 11.3, 11.6, 12.0, 11.7, 11.2, 12.2, 11.7, 12.1, 12.5, 11.7, 12.7, 12.5, 13.0, 12.2, 12.7, 12.5, 12.8, 11.1, 11.7, 10.6, 11.3),
        12 to listOf(10.5, 8.9, 10.2, 9.2, 10.9, 10.9, 11.6, 10.2, 11.5, 8.9, 10.9, 10.8, 11.2, 11.6, 11.4, 11.7, 12.0, 11.7, 11.1, 11.9, 11.5, 9.8, 10.2, 10.1)
    )

    fun loadFromCsv(context: Context) {
        if (isLoaded) return
        val counts = Array(13) { IntArray(24) { 0 } }
        val sums = Array(13) { DoubleArray(24) { 0.0 } }

        try {
            context.assets.open("marine_data.csv").bufferedReader().useLines { lines ->
                var isFirst = true
                lines.forEach { line ->
                    if (isFirst) {
                        isFirst = false
                        return@forEach
                    }
                    val parts = line.split(",")
                    if (parts.size >= 9) {
                        val month = parts[1].trim().toIntOrNull() ?: 1
                        val hour = parts[3].trim().toIntOrNull() ?: 0
                        val windSpeed = parts[7].trim().toDoubleOrNull() ?: -9.9
                        if (windSpeed >= 0.0 && month in 1..12 && hour in 0..23) {
                            sums[month][hour] += windSpeed
                            counts[month][hour] += 1
                        }
                    }
                }
            }
            for (m in 1..12) {
                val list = List(24) { h ->
                    val count = counts[m][h]
                    if (count > 0) Math.round((sums[m][h] / count) * 10.0) / 10.0 else (fallbackAverages[m]?.get(h) ?: 5.0)
                }
                monthlyHourlyAverages[m] = list
            }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            monthlyHourlyAverages.putAll(fallbackAverages)
            isLoaded = true
        }
    }

    fun getForMonth(context: Context, month: Int): List<Double> {
        loadFromCsv(context)
        return monthlyHourlyAverages[month] ?: fallbackAverages[month] ?: List(24) { 0.0 }
    }

    // High fidelity color palettes representing the image chart styles
    // Period colors:
    // 3-Month group colors: Month 1 is Dark Navy/Blue, Month 2 is Orange, Month 3 is Dark Green
    val color3MonthNavy = Color(0xFF1B4D72)   // Premium Deep Navy
    val color3MonthOrange = Color(0xFFC85E29) // Warm Burnt Orange
    val color3MonthGreen = Color(0xFF1E522B)  // Forest Green

    // 6-Month group colors:
    val color6MonthBlue = Color(0xFF3B6EB5)    // Muted blue
    val color6MonthOrange = Color(0xFFE48243)  // Bright orange
    val color6MonthGrey = Color(0xFF8C959D)    // Charcoal
    val color6MonthYellow = Color(0xFFE4AE12)  // Mustard Gold
    val color6MonthLightBlue = Color(0xFF4FA0D8)// Soft blue
    val color6MonthGreen = Color(0xFF6F9D4E)   // Sage green

    // Label descriptions for quarters
    val quarterLabels = listOf(
        "Q1春季 (1~3月平均風速)",
        "Q2夏季 (4~6月平均風速)",
        "Q3秋季 (7~9月平均風速)",
        "Q4冬季 (10~12月平均風速)"
    )

    // Label descriptions for halves
    val halfLabels = listOf(
        "1~6月平均風速 (上半年份)",
        "7~12月平均風速 (下半年份)"
    )
}

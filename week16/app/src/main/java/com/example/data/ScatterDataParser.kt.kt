package com.example.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.sqrt

object ScatterDataParser {
    private var isLoaded = false

    // 儲存每個月份（1~12）對應的 List<Pair<風速, 波高>>
    private val monthlyScatterPoints = mutableMapOf<Int, List<Pair<Double, Double>>>()
    // 儲存每個月份動態計算出來的 Pearson 相關係數 r
    private val monthlyPearsonR = mutableMapOf<Int, Double>()

    /**
     * 開啟 assets 中的 marine_data.csv 並進行中文欄位解析
     */
    fun loadFromCsv(context: Context) {
        if (isLoaded) return // 如果已經載入過，就直接使用快取

        try {
            // 指定使用 UTF-8 編碼讀取中文
            context.assets.open("marine_data.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val header = reader.readLine() ?: return
                    val headers = header.split(",")

                    // 🔍 精確比對你提供的 CSV 中文標題欄位
                    val monthIdx = headers.indexOf("月")
                    val windSpeedIdx = headers.indexOf("風速")
                    val waveHeightIdx = headers.indexOf("波高")

                    // 安全防呆：如果找不到對應欄位則不處理
                    if (monthIdx == -1 || windSpeedIdx == -1 || waveHeightIdx == -1) {
                        return
                    }

                    // 初始化 1 到 12 月的暫存容器
                    val tempMap = mutableMapOf<Int, MutableList<Pair<Double, Double>>>()
                    for (m in 1..12) {
                        tempMap[m] = mutableListOf()
                    }

                    // 逐行讀取資料
                    reader.forEachLine { line ->
                        val tokens = line.split(",")
                        if (tokens.size > maxOf(monthIdx, windSpeedIdx, waveHeightIdx)) {
                            val month = tokens[monthIdx].toIntOrNull()
                            val windSpeed = tokens[windSpeedIdx].toDoubleOrNull()
                            val waveHeight = tokens[waveHeightIdx].toDoubleOrNull()

                            // 資料防呆，確保數值正確且月份在 1~12 之間
                            if (month != null && windSpeed != null && waveHeight != null && month in 1..12) {
                                tempMap[month]?.add(Pair(windSpeed, waveHeight))
                            }
                        }
                    }

                    // 將解析完成的數據轉存入正式快取，並同步計算該月份的實時 Pearson R 相關係數
                    tempMap.forEach { (m, points) ->
                        monthlyScatterPoints[m] = points
                        monthlyPearsonR[m] = calculatePearsonR(points)
                    }
                    isLoaded = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 提供外部獲取某月份的 (風速, 波高) 點集合
     */
    fun getPointsForMonth(context: Context, month: Int): List<Pair<Double, Double>> {
        loadFromCsv(context)
        return monthlyScatterPoints[month] ?: emptyList()
    }

    /**
     * 提供外部獲取某月份由真實 CSV 算出來的相關係數
     */
    fun getPearsonRForMonth(context: Context, month: Int): Double {
        loadFromCsv(context)
        return monthlyPearsonR[month] ?: 0.0
    }

    /**
     * 數學公式：動態計算 Pearson 相關係數
     */
    private fun calculatePearsonR(points: List<Pair<Double, Double>>): Double {
        if (points.size < 2) return 0.0
        val n = points.size
        val sumX = points.sumOf { it.first }
        val sumY = points.sumOf { it.second }
        val sumXSquare = points.sumOf { it.first * it.first }
        val sumYSquare = points.sumOf { it.second * it.second }
        val sumXY = points.sumOf { it.first * it.second }

        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumXSquare - sumX * sumX) * (n * sumYSquare - sumY * sumY))

        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
}
package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MarineRecord
import com.example.data.MarineRepository
import com.example.data.ScatterDataParser
import com.example.data.HourlyWindData
import com.example.network.CwaApiClient
import com.example.utils.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

sealed interface CwaState {
    object Loading : CwaState
    data class Success(val record: MarineRecord, val isSimulated: Boolean) : CwaState
    data class Error(val message: String) : CwaState
}

class MarineViewModel(
    private val repository: MarineRepository,
    private val application: Application
) : ViewModel() {

    private val context = application.applicationContext
    private val sharedPrefs = application.getSharedPreferences("marine_prefs", Context.MODE_PRIVATE)

    // Api Key State
    private val _apiKey = MutableStateFlow(sharedPrefs.getString("cwa_api_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // Real-time API state
    private val _cwaState = MutableStateFlow<CwaState>(CwaState.Loading)
    val cwaState: StateFlow<CwaState> = _cwaState.asStateFlow()

    // Scatter plot selected month (1 - 12)
    private val _selectedScatterMonth = MutableStateFlow(10)
    val selectedScatterMonth: StateFlow<Int> = _selectedScatterMonth.asStateFlow()

    // Bar chart period (3 months or 6 months)
    private val _selectedChartPeriod = MutableStateFlow(3)
    val selectedChartPeriod: StateFlow<Int> = _selectedChartPeriod.asStateFlow()

    // Room official observations flow
    val officialObservations: StateFlow<List<MarineRecord>> = repository.officialObservations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room user patrol logs list flow
    val userNotes: StateFlow<List<MarineRecord>> = repository.userNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room favorite records flow
    val favoriteRecords: StateFlow<List<MarineRecord>> = repository.favoriteRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- History Query States ---
    val historySearchQuery = MutableStateFlow("")
    val historySelectedMonth = MutableStateFlow(0)
    val historySelectedYear = MutableStateFlow(0)
    val historyMinWindSpeed = MutableStateFlow(0f)
    val historyMinWaveHeight = MutableStateFlow(0f)

    // 🔧 徹底重做處：使用 flatMapLatest 將篩選狀態跟資料庫查詢綁定（加上 150ms 的防抖，避免連續頻繁查詢）
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredHistoryRecords: StateFlow<List<MarineRecord>> = combine(
        historySearchQuery,
        historySelectedYear,
        historySelectedMonth,
        historyMinWindSpeed,
        historyMinWaveHeight
    ) { query, year, month, minWind, minWave ->
        // 打包過濾參數
        IndexedValue(0, listOf(query, year.toString(), month.toString(), minWind.toString(), minWave.toString()))
    }
        .debounce(150) // 拉動滑桿時的微型防抖，保證效能
        .flatMapLatest {
            repository.getFilteredHistory(
                query = historySearchQuery.value,
                year = historySelectedYear.value,
                month = historySelectedMonth.value,
                minWind = historyMinWindSpeed.value.toDouble(),
                minWave = historyMinWaveHeight.value.toDouble()
            )
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 散佈圖數據點
    val scatterPoints: StateFlow<List<Pair<Double, Double>>> = _selectedScatterMonth
        .map { month ->
            ScatterDataParser.getPointsForMonth(context, month)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 相關係數 r
    val pearsonR: StateFlow<Double> = _selectedScatterMonth
        .map { month ->
            ScatterDataParser.getPearsonRForMonth(context, month)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 0.0)

    // --- Averages (Bar Chart) States ---
    data class AverageBar(val label: String, val avgWindSpeed: Double)

    val averageWindSpeedBars: StateFlow<List<AverageBar>> = _selectedScatterMonth
        .map { month ->
            val hourlyList = HourlyWindData.getForMonth(context, month)
            hourlyList.mapIndexed { hour, avgSpeed ->
                AverageBar(label = "${hour}時", avgWindSpeed = avgSpeed)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User current Location GPS ---
    val userLatitude = MutableStateFlow<Double?>(null)
    val userLongitude = MutableStateFlow<Double?>(null)
    val userDistanceKm = MutableStateFlow<Double?>(null)
    val locationStatusMessage = MutableStateFlow("尚未定位")

    init {
        fetchRealTimeData()

        // 💡 啟動 App 時，在背景檢查資料庫。若裡面沒有資料，就一次性把 CSV 灌入 Room 中
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (repository.getCount() == 0) {
                    val initialRecords = com.example.data.MarineDatabase.generateInitialRecordsFromCsv(context)
                    repository.insertAll(initialRecords)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- CWA API Key setter ---
    fun updateApiKey(newKey: String) {
        _apiKey.value = newKey
        sharedPrefs.edit().putString("cwa_api_key", newKey).apply()
        fetchRealTimeData()
    }

    // --- Fetch Real-time Marine Weather ---
    fun fetchRealTimeData() {
        _cwaState.value = CwaState.Loading
        viewModelScope.launch {
            try {
                if (_apiKey.value.isBlank()) {
                    _cwaState.value = CwaState.Success(generateSimulatedData(), isSimulated = true)
                    return@launch
                }

                val response = CwaApiClient.apiService.getMarineObservations(_apiKey.value)
                val locationList = response.records?.seaObs?.location

                val portLoc = locationList?.firstOrNull {
                    it.locationName?.contains("台中港") == true || it.stationName?.contains("台中港") == true
                } ?: locationList?.firstOrNull()

                if (portLoc != null) {
                    val primary = portLoc.weatherElement?.primary
                    val waveHeight = primary?.waveHeight?.toDoubleOrNull() ?: 1.3
                    val wavePeriod = primary?.wavePeriod?.toDoubleOrNull() ?: 6.2
                    val waveDir = primary?.waveDirection ?: "NNE"
                    val windSpeed = primary?.windSpeed?.toDoubleOrNull() ?: 11.5
                    val windDir = primary?.windDirection ?: "NNE"
                    val obsTime = portLoc.obsTime?.obsTime ?: "即時"

                    val formattedTime = formatCwaTime(obsTime)

                    val record = MarineRecord(
                        time = formattedTime,
                        year = 2026,
                        month = 6,
                        waveHeight = waveHeight,
                        wavePeriod = wavePeriod,
                        waveDirection = waveDir,
                        windSpeed = windSpeed,
                        windDirection = windDir
                    )
                    _cwaState.value = CwaState.Success(record, isSimulated = false)
                } else {
                    _cwaState.value = CwaState.Success(generateSimulatedData(), isSimulated = true)
                }
            } catch (e: Exception) {
                _cwaState.value = CwaState.Success(generateSimulatedData(), isSimulated = true)
            }
        }
    }

    private fun generateSimulatedData(): MarineRecord {
        val rand = Random(System.currentTimeMillis())
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentTime = dateFormatter.format(Date())

        val simWind = Math.round((7.5 + rand.nextDouble(-2.0, 3.5)) * 10.0) / 10.0
        val simWave = Math.round((1.1 + rand.nextDouble(-0.3, 0.5)) * 10.0) / 10.0
        val simPeriod = Math.round((4.8 + simWave * 1.2) * 10.0) / 10.0
        val speedDirection = listOf("N", "NNE", "NE", "ENE", "SSW", "SW", "S", "WSW").random(rand)
        val waveDirection = listOf("NNE", "NE", "SSW", "SW").random(rand)

        return MarineRecord(
            time = currentTime,
            year = 2026,
            month = 6,
            waveHeight = simWave,
            wavePeriod = simPeriod,
            waveDirection = waveDirection,
            windSpeed = simWind,
            windDirection = speedDirection
        )
    }

    private fun formatCwaTime(rawTime: String): String {
        return try {
            if (rawTime.contains("T")) {
                val clean = rawTime.substringBefore("+").replace("T", " ")
                clean.substring(0, 16)
            } else {
                rawTime
            }
        } catch (e: Exception) {
            rawTime
        }
    }

    // --- Favorite Interactions ---
    fun toggleFavorite(record: MarineRecord) {
        viewModelScope.launch {
            repository.update(record.copy(isFavorite = !record.isFavorite))
        }
    }

    // --- Weather Selection Handlers ---
    fun setScatterMonth(month: Int) {
        _selectedScatterMonth.value = month
    }

    fun setChartPeriod(period: Int) {
        _selectedChartPeriod.value = period
    }

    // --- User Notes / Camera Patrol Records ---
    fun saveUserPatrolNote(description: String, photoPath: String?) {
        viewModelScope.launch {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateString = df.format(Date())

            val lat = userLatitude.value
            val lon = userLongitude.value

            val noteRecord = MarineRecord(
                time = dateString,
                year = Calendar.getInstance().get(Calendar.YEAR),
                month = Calendar.getInstance().get(Calendar.MONTH) + 1,
                waveHeight = 0.0,
                wavePeriod = 0.0,
                waveDirection = "N/A",
                windSpeed = 0.0,
                windDirection = "N/A",
                isFavorite = false,
                isUserNote = true,
                userNoteText = description,
                userPhotoPath = photoPath,
                latitude = lat,
                longitude = lon
            )
            repository.insert(noteRecord)
        }
    }

    fun deleteUserNote(record: MarineRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    // --- Refresh/Fetch GPS ---
    fun triggerGPSLocation(context: Context) {
        locationStatusMessage.value = "計算定位中..."
        LocationHelper.getCurrentLocation(
            context = context,
            onSuccess = { lat, lon ->
                userLatitude.value = lat
                userLongitude.value = lon
                val dist = LocationHelper.calculateDistanceKm(lat, lon)
                userDistanceKm.value = Math.round(dist * 100.0) / 100.0
                locationStatusMessage.value = "定位成功！"
            },
            onFailure = { error ->
                locationStatusMessage.value = error
            }
        )
    }
}

class MarineViewModelFactory(
    private val repository: MarineRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarineViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
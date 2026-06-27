package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class CwaResponse(
    @Json(name = "success") val success: String,
    @Json(name = "records") val records: CwaRecords?
)

@JsonClass(generateAdapter = true)
data class CwaRecords(
    @Json(name = "seaObs") val seaObs: SeaObs?
)

@JsonClass(generateAdapter = true)
data class SeaObs(
    @Json(name = "location") val location: List<SeaLocation>?
)

@JsonClass(generateAdapter = true)
data class SeaLocation(
    @Json(name = "stationId") val stationId: String?,
    @Json(name = "locationName") val locationName: String?,
    @Json(name = "stationName") val stationName: String?,
    @Json(name = "lat") val lat: String?,
    @Json(name = "lon") val lon: String?,
    @Json(name = "obsTime") val obsTime: ObsTime?,
    @Json(name = "weatherElement") val weatherElement: WeatherElement?
)

@JsonClass(generateAdapter = true)
data class ObsTime(
    @Json(name = "obsTime") val obsTime: String?
)

@JsonClass(generateAdapter = true)
data class WeatherElement(
    @Json(name = "primary") val primary: WeatherPrimary?
)

@JsonClass(generateAdapter = true)
data class WeatherPrimary(
    @Json(name = "waveHeight") val waveHeight: String?,
    @Json(name = "wavePeriod") val wavePeriod: String?,
    @Json(name = "waveDirection") val waveDirection: String?,
    @Json(name = "windSpeed") val windSpeed: String?,
    @Json(name = "windDirection") val windDirection: String?
)

interface CwaApiService {
    @GET("api/v1/rest/datastore/O-A0019-001")
    suspend fun getMarineObservations(
        @Query("Authorization") apiKey: String,
        @Query("limit") limit: Int = 10,
        @Query("format") format: String = "JSON"
    ): CwaResponse
}

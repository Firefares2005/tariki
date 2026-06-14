package com.example.core.utils

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader

object RouteUtils {
    suspend fun getSearchSuggestions(query: String): List<Triple<String, Double, Double>> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<Triple<String, Double, Double>>()
            // 1. Try Photon Komoot API as it is super robust, quick, and rarely blocked on cloud IP addresses
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val urlString = "https://photon.komoot.io/api/?q=$encodedQuery&limit=8"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TarikiApp/1.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val features = json.optJSONArray("features")
                    if (features != null) {
                        for (i in 0 until features.length()) {
                            val feat = features.getJSONObject(i)
                            val geometry = feat.optJSONObject("geometry")
                            val properties = feat.optJSONObject("properties")
                            if (geometry != null && properties != null) {
                                val coordinates = geometry.optJSONArray("coordinates")
                                if (coordinates != null && coordinates.length() >= 2) {
                                    val lon = coordinates.getDouble(0)
                                    val lat = coordinates.getDouble(1)
                                    
                                    val name = properties.optString("name", "")
                                    val city = properties.optString("city", "")
                                    val country = properties.optString("country", "")
                                    
                                    val displayName = if (name.isNotBlank()) {
                                        if (city.isNotBlank()) "$name, $city" else name
                                    } else {
                                        properties.optString("city", properties.optString("country", "مكان غير معروف"))
                                    }
                                    if (displayName.isNotBlank()) {
                                        list.add(Triple(displayName, lat, lon))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Return empty list on any failure
            }

            // 2. If list is empty, fall back to Nominatim OpenStreetMap
            if (list.isEmpty()) {
                try {
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1&accept-language=ar"
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "TarikiApp/1.0")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()

                        val array = JSONArray(response)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val displayName = obj.optString("display_name", "")
                            val lat = obj.optDouble("lat", 0.0)
                            val lon = obj.optDouble("lon", 0.0)
                            if (displayName.isNotEmpty() && lat != 0.0 && lon != 0.0) {
                                // Trim display name up to 3 segments for visual clarity in chips
                                val segments = displayName.split(",")
                                val shortName = if (segments.size > 2) {
                                    "${segments[0].trim()}, ${segments[1].trim()}"
                                } else {
                                    displayName.trim()
                                }
                                list.add(Triple(shortName, lat, lon))
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore on any failure
                }
            }
            list
        }
    }

    suspend fun fetchRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double): List<LatLng> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=polyline"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TarikiApp/1.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).getString("geometry")
                        val decoded = decodePolyline(geometry)
                        if (decoded.isNotEmpty()) {
                            return@withContext decoded
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore failure
            }
            // Fallback to direct path between pickup and destination if OSRM is blocked or unavailable
            listOf(LatLng(startLat, startLng), LatLng(endLat, endLng))
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = java.util.ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val requestLat = lat / 1E5
            val requestLng = lng / 1E5
            poly.add(LatLng(requestLat, requestLng))
        }
        return poly
    }
}

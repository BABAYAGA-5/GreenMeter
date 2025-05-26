package com.example.greenmeter.service

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.greenmeter.BuildConfig
import com.example.greenmeter.model.Weather
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherService(private val context: Context) {
    private val apiKey = BuildConfig.WEATHER_API_KEY.trim() // Get API key from BuildConfig and trim any spaces
    private val baseUrl = "https://api.openweathermap.org/data/2.5/weather"

    private fun checkLocationPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("WeatherService", "Location permission check: $hasPermission")
        return hasPermission
    }

    private suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        Log.d("WeatherService", "Getting current location...")
        suspendCancellableCoroutine { continuation ->
            try {
                if (!checkLocationPermission()) {
                    Log.e("WeatherService", "Location permission not granted")
                    continuation.resumeWithException(Exception("Location permission not granted"))
                    return@suspendCancellableCoroutine
                }

                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                
                // Create a location request with strict criteria
                val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                    .setDurationMillis(5000) // 5 seconds timeout
                    .setMaxUpdateAgeMillis(0) // Force fresh location
                    .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                val cancellationToken = com.google.android.gms.tasks.CancellationTokenSource().token

                Log.d("WeatherService", "Requesting fresh location with high accuracy...")
                fusedLocationClient.getCurrentLocation(locationRequest, cancellationToken)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val locationAge = System.currentTimeMillis() - location.time
                            Log.d("WeatherService", "Got fresh location: " +
                                "lat=${location.latitude}, lon=${location.longitude}, " +
                                "accuracy=${location.accuracy}m, " +
                                "age=${locationAge/1000} seconds, " +
                                "provider=${location.provider}")

                            if (location.accuracy <= 100 && locationAge < 30000) { // Less than 100m accuracy and 30 seconds old
                                continuation.resume(location)
                            } else {
                                Log.d("WeatherService", "Location not accurate enough or too old, requesting location updates...")
                                requestLocationUpdates(continuation, fusedLocationClient)
                            }
                        } else {
                            Log.d("WeatherService", "getCurrentLocation returned null, requesting location updates...")
                            requestLocationUpdates(continuation, fusedLocationClient)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("WeatherService", "Failed to get current location", e)
                        requestLocationUpdates(continuation, fusedLocationClient)
                    }
            } catch (e: SecurityException) {
                Log.e("WeatherService", "Security exception in getCurrentLocation", e)
                continuation.resumeWithException(Exception("Location permission not granted"))
            }
        }
    }

    private fun requestLocationUpdates(
        continuation: kotlin.coroutines.Continuation<Location?>,
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    ) {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(5000L) // 5 seconds interval
            .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(0f)
            .setMinUpdateIntervalMillis(1000) // 1 second minimum interval
            .setMaxUpdateDelayMillis(5000) // 5 seconds maximum delay
            .build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.d("WeatherService", "Got location update: " +
                        "lat=${location.latitude}, lon=${location.longitude}, " +
                        "accuracy=${location.accuracy}m, " +
                        "time=${java.util.Date(location.time)}, " +
                        "provider=${location.provider}")
                    
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e("WeatherService", "Failed to request location updates", e)
                tryLocationManager(continuation)
            }

            // Set a timeout for location updates
            Handler(Looper.getMainLooper()).postDelayed({
                fusedLocationClient.removeLocationUpdates(locationCallback)
                tryLocationManager(continuation)
            }, 15000) // 15 seconds timeout
        } catch (e: SecurityException) {
            Log.e("WeatherService", "Security exception requesting location updates", e)
            tryLocationManager(continuation)
        }
    }

    private fun tryLocationManager(continuation: kotlin.coroutines.Continuation<Location?>) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            
            Log.d("WeatherService", "Trying LocationManager with providers: $providers")
            
            // Keep track of active location listeners
            val activeListeners = mutableListOf<android.location.LocationListener>()
            var timeoutHandler: Handler? = null
            
            // Create a function to clean up listeners
            fun cleanup() {
                activeListeners.forEach { listener ->
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (e: Exception) {
                        Log.e("WeatherService", "Error removing location updates", e)
                    }
                }
                activeListeners.clear()
                timeoutHandler?.removeCallbacksAndMessages(null)
            }

            for (provider in providers) {
                try {
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(location: Location) {
                            Log.d("WeatherService", "Got location update from $provider: " +
                                "lat=${location.latitude}, lon=${location.longitude}, " +
                                "accuracy=${location.accuracy}m")
                            
                            if (location.accuracy <= 100) { // Good enough accuracy
                                cleanup()
                                continuation.resume(location)
                            }
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    
                    activeListeners.add(listener)
                    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (e: Exception) {
                    Log.e("WeatherService", "Error requesting updates from $provider", e)
                }
            }

            // Set a timeout
            timeoutHandler = Handler(Looper.getMainLooper())
            timeoutHandler.postDelayed({
                // Get the best location from all providers
                var bestLocation: Location? = null
                var bestAccuracy = Float.MAX_VALUE

                for (provider in providers) {
                    try {
                        val location = locationManager.getLastKnownLocation(provider)
                        if (location != null && (bestLocation == null || location.accuracy < bestAccuracy)) {
                            bestLocation = location
                            bestAccuracy = location.accuracy
                        }
                    } catch (e: SecurityException) {
                        Log.e("WeatherService", "Security exception getting location from $provider", e)
                    }
                }

                cleanup()

                if (bestLocation != null) {
                    Log.d("WeatherService", "Using best available location after timeout: " +
                        "lat=${bestLocation.latitude}, lon=${bestLocation.longitude}, " +
                        "accuracy=${bestLocation.accuracy}m")
                    continuation.resume(bestLocation)
                } else {
                    Log.e("WeatherService", "Could not get location from any provider")
                    continuation.resumeWithException(Exception("Could not get location"))
                }
            }, 15000) // 15 seconds timeout
        } catch (e: SecurityException) {
            Log.e("WeatherService", "Security exception getting location", e)
            continuation.resumeWithException(Exception("Location permission not granted"))
        }
    }

    private fun getWindDirection(degrees: Int): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((degrees + 22.5) % 360 / 45).toInt()
        return directions[index]
    }

    suspend fun getWeatherData(): Weather = withContext(Dispatchers.IO) {
        try {
            Log.d("WeatherService", "Starting weather data fetch")
            if (apiKey == "YOUR_API_KEY" || apiKey.isEmpty()) {
                Log.e("WeatherService", "API key not configured")
                throw Exception("Weather API key not configured")
            }

            // Hardcoded coordinates for Rabat, Morocco for emulator testing
            val latitude = 34.0209
            val longitude = -6.8416

            Log.d("WeatherService", "Using coordinates for Rabat: lat=$latitude, lon=$longitude")

            // Construct URL with proper encoding
            val url = buildString {
                append(baseUrl)
                append("?lat=").append(latitude)
                append("&lon=").append(longitude)
                append("&appid=").append(URLEncoder.encode(apiKey, "UTF-8"))
                append("&units=metric")
            }

            Log.d("WeatherService", "Fetching weather from URL: $url")
            
            val response = URL(url).readText()
            Log.d("WeatherService", "Got weather response: $response")
            
            val jsonObj = JSONObject(response)

            if (!jsonObj.has("main")) {
                Log.e("WeatherService", "Invalid weather data received")
                throw Exception("Invalid weather data received")
            }

            val main = jsonObj.getJSONObject("main")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
            val wind = jsonObj.getJSONObject("wind")
            val sys = jsonObj.getJSONObject("sys")
            val name = jsonObj.getString("name")

            Weather(
                temperature = main.getDouble("temp"),
                condition = weather.getString("main"),
                humidity = main.getInt("humidity"),
                visibility = jsonObj.getInt("visibility") / 1000, // Convert to km
                windSpeed = wind.getDouble("speed"),
                windDirection = getWindDirection(wind.getInt("deg")),
                location = "$name, ${sys.getString("country")}"
            ).also {
                Log.d("WeatherService", "Successfully created Weather object: $it")
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Error fetching weather data", e)
            throw e
        }
    }
} 
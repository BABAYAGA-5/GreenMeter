package com.example.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.example.greenmeter.R
import com.example.greenmeter.model.Device
import com.example.greenmeter.model.Weather
import com.example.greenmeter.service.WeatherService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val weatherService = remember { WeatherService(context) }
    var weather by remember { mutableStateOf<Weather?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Permission handling
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
        if (hasLocationPermission) {
            // Fetch weather data after permission is granted
            scope.launch {
                try {
                    withContext(Dispatchers.Main) {
                        isLoading = true
                        errorMessage = null
                    }
                    val weatherData = withContext(Dispatchers.IO) {
                        weatherService.getWeatherData()
                    }
                    withContext(Dispatchers.Main) {
                        Log.d("HomeScreen", "Updating weather state with data: $weatherData")
                        weather = weatherData
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error fetching weather data", e)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = when {
                            e.message?.contains("API key") == true -> "Weather service configuration error"
                            else -> "Error loading weather data"
                        }
                    }
                }
            }
        } else {
            isLoading = false
            errorMessage = "Location permission required for weather data"
        }
    }

    // Request location permission and fetch weather on first launch
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // If we already have permission, fetch weather data
            try {
                withContext(Dispatchers.Main) {
                    isLoading = true
                    errorMessage = null
                }
                val weatherData = withContext(Dispatchers.IO) {
                    weatherService.getWeatherData()
                }
                withContext(Dispatchers.Main) {
                    Log.d("HomeScreen", "Updating weather state with data: $weatherData")
                    weather = weatherData
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error fetching weather data", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = when {
                        e.message?.contains("API key") == true -> "Weather service configuration error"
                        else -> "Error loading weather data"
                    }
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val context = LocalContext.current

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.home_screen, null)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                view
            },
            update = { view ->
                val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
                logoutButton.setOnClickListener {
                    Log.d("HomeScreen", "Logout clicked")
                    Firebase.auth.signOut()
                    navController.navigate("login")
                }
                val greetingText = view.findViewById<TextView>(R.id.greetingText)
                val datetime = view.findViewById<TextView>(R.id.datetime)
                val curentdatetime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                datetime.text = curentdatetime
                val currenttime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                when (currenttime) {
                    in "00:00:00".."06:00:00" -> greetingText.text = "Good Night"
                    in "06:00:01".."12:00:00" -> greetingText.text = "Good Morning"
                    in "12:00:01".."18:00:00" -> greetingText.text = "Good Afternoon"
                    in "18:00:01".."23:59:59" -> greetingText.text = "Good Evening"
                }
                val displayName = view.findViewById<TextView>(R.id.displayName)
                displayName.text = "${Firebase.auth.currentUser?.displayName}"

                // Weather card views
                val weatherStatus = view.findViewById<TextView>(R.id.weatherStatus)
                val location = view.findViewById<TextView>(R.id.location)
                val temperature = view.findViewById<TextView>(R.id.temperature)
                val humidity = view.findViewById<TextView>(R.id.humidity)
                val visibility = view.findViewById<TextView>(R.id.visibility)
                val windSpeed = view.findViewById<TextView>(R.id.windSpeed)
                val windDirection = view.findViewById<TextView>(R.id.windDirection)

                // Update weather data based on state
                when {
                    isLoading -> {
                        Log.d("HomeScreen", "UI State: Loading")
                        weatherStatus.text = "Loading weather data..."
                        location.text = "Please wait"
                        temperature.text = "--°C"
                        humidity.text = "--%"
                        visibility.text = "-- km"
                        windSpeed.text = "-- km/h"
                        windDirection.text = "-- Wind"
                    }
                    errorMessage != null -> {
                        Log.d("HomeScreen", "UI State: Error - $errorMessage")
                        weatherStatus.text = errorMessage
                        location.text = "Error"
                        temperature.text = "--°C"
                        humidity.text = "--%"
                        visibility.text = "-- km"
                        windSpeed.text = "-- km/h"
                        windDirection.text = "-- Wind"
                    }
                    weather != null -> {
                        Log.d("HomeScreen", "UI State: Weather Data Available - $weather")
                        weatherStatus.text = weather?.condition
                        location.text = weather?.location
                        temperature.text = "${weather?.temperature?.toInt()}°C"
                        humidity.text = "${weather?.humidity}%"
                        visibility.text = "${weather?.visibility} km"
                        windSpeed.text = "${weather?.windSpeed?.toInt()} km/h"
                        windDirection.text = "${weather?.windDirection} Wind"
                    }
                }

                val devicesContainer = view.findViewById<GridLayout>(R.id.devicesContainer)
                // Clear existing views
                devicesContainer.removeAllViews()
                
                val deviceWidthDp = context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density
                val cardWidth = (deviceWidthDp * 0.95f).toInt() // 90% of screen width in dp
                val contentContainer = view.findViewById<LinearLayout>(R.id.contentContainer)

                fun addCard(device: Device?) {
                    val cardView = CardView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            cardWidth,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(16, 8, 16, 8)
                            gravity = android.view.Gravity.CENTER
                        }
                        cardElevation = 4f
                        radius = 16f
                        setCardBackgroundColor(ContextCompat.getColor(context, R.color.card))
                    }

                    // Main layout that holds both image and text
                    val mainLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                    }

                    val imageView = ImageView(context).apply {
                        setImageResource(
                            when (device?.deviceLogo) {
                                "bedroom" -> R.drawable.bedroom
                                "kitchen" -> R.drawable.kitchen
                                "living_room" -> R.drawable.livingroom
                                "bathroom" -> R.drawable.bathroom
                                "add" -> R.drawable.add
                                else -> R.drawable.question_mark
                            }
                        )
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            200
                        )
                    }

                    val textView = TextView(context).apply {
                        text = device?.deviceName
                        textSize = 16f
                        gravity = android.view.Gravity.CENTER
                        setTextColor(ContextCompat.getColor(context, R.color.black))
                        textSize = 24f
                    }

                    // Add views to main layout
                    mainLayout.addView(imageView)
                    mainLayout.addView(textView)

                    // Add main layout to card
                    cardView.addView(mainLayout)

                    // Add card to container
                    devicesContainer.addView(cardView)
                    cardView.setOnClickListener {
                        Log.d("HomeScreen", "Card clicked for device: ${device?.deviceId}")
                        if (device?.deviceId == "new") {
                            Log.d("HomeScreen", "Adding new device")
                            navController.navigate("AddDevice")
                        } else {
                            Log.d("HomeScreen", "Navigating to device details for: ${device?.deviceId}")
                            navController.navigate("details/${device?.deviceId}")
                        }
                    }
                }

                val devicesList = mutableListOf<Device>()
                val db = FirebaseDatabase.getInstance()
                val userId = Firebase.auth.currentUser?.uid ?: ""
                val devicesRef = db.getReference("devices")

                Log.d("Firebase", "Fetching devices for user: $userId")
                devicesRef.get().addOnSuccessListener { snapshot ->
                    // Clear both the list and the container before adding new devices
                    devicesList.clear()
                    devicesContainer.removeAllViews()
                    
                    if (snapshot.exists()) {
                        Log.d("Firebase", "Devices exist, filtering for user: $userId")
                        for (deviceSnap in snapshot.children) {
                            val deviceUserId = deviceSnap.child("userId").getValue(String::class.java)
                            if (deviceUserId == userId) {
                                val deviceId = deviceSnap.key ?: ""
                                val deviceName = deviceSnap.child("deviceName").getValue(String::class.java) ?: "unknown"
                                val deviceLogo = deviceSnap.child("deviceLogo").getValue(String::class.java) ?: "unknown"
                                devicesList.add(Device(deviceId, deviceName, deviceLogo))
                                Log.d("Firebase", "Device found: ${deviceName} with ID: ${deviceId} and logo: ${deviceLogo}")
                            }
                        }
                        
                        // Add all found devices to the UI
                        for (device in devicesList) {
                            addCard(device)
                        }
                        Log.d("Firebase", "Total devices found for user: ${devicesList.size}")
                        
                        // Add the "New device" card
                        val addDevice = Device("new", "New device", "add")
                        addCard(addDevice)
                    } else {
                        Log.d("Firebase", "No devices found in database")
                        // Add just the "New device" card when no devices exist
                        val addDevice = Device("new", "New device", "add")
                        addCard(addDevice)
                    }
                }.addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to get devices", e)
                    // Clear views before adding the new device card
                    devicesContainer.removeAllViews()
                    // Add the "New device" card even on failure
                    val addDevice = Device("new", "New device", "add")
                    addCard(addDevice)
                }

                val profileButton = view.findViewById<ImageButton>(R.id.profileButton)
                val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
                val analyticsButton = view.findViewById<ImageButton>(R.id.analyticsButton)

                analyticsButton.setOnClickListener {
                    Log.d("HomeScreen", "Analytics clicked")
                    navController.navigate("analytics")
                }

                profileButton.setOnClickListener {
                    Log.d("HomeScreen", "Profile clicked")
                    navController.navigate("profile")
                }

                homeButton.setOnClickListener {
                    Log.d("HomeScreen", "Home clicked")
                    // Already on Home Screen, no action needed
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
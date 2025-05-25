package com.example.greenmeter.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import com.example.greenmeter.views.PowerReading
import com.example.greenmeter.views.MonthlyEnergyData
import com.example.greenmeter.views.UsageGraph
import com.example.greenmeter.views.MonthlyEnergyGraph
import java.util.Calendar
import kotlin.math.roundToInt

data class DeviceConsumption(
    val deviceId: String = "",
    val deviceName: String = "",
    val currentPower: Int = 0,
    val monthlyEnergy: Float = 0f,
    val powerHistory: List<PowerReading> = emptyList(),
    val monthlyEnergyHistory: List<MonthlyEnergyData> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var totalCurrentPower by remember { mutableStateOf(0) }
    var totalMonthlyEnergy by remember { mutableStateOf(0f) }
    var devicesList by remember { mutableStateOf(listOf<DeviceConsumption>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var combinedPowerReadings by remember { mutableStateOf(listOf<PowerReading>()) }
    var combinedMonthlyEnergy by remember { mutableStateOf(listOf<MonthlyEnergyData>()) }
    var weeklyAverage by remember { mutableStateOf(0f) }
    var monthlyAverage by remember { mutableStateOf(0f) }

    // Function to calculate averages
    fun calculateAverages(readings: List<PowerReading>) {
        if (readings.isEmpty()) {
            weeklyAverage = 0f
            monthlyAverage = 0f
            return
        }

        // Calculate weekly average (last 7 days) in kWh
        weeklyAverage = readings.takeLast(7).map { it.value }.average().toFloat() / 1000f

        // Calculate monthly average (last 30 days) in kWh
        monthlyAverage = readings.takeLast(30).map { it.value }.average().toFloat() / 1000f
    }

    // Set up real-time listener
    DisposableEffect(Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        val devicesRef = FirebaseDatabase.getInstance().getReference("devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val devices = mutableListOf<DeviceConsumption>()
                    var currentPowerSum = 0
                    var monthlyEnergySum = 0f
                    val powerReadingsMap = mutableMapOf<String, Float>() // timestamp -> total power
                    val monthlyEnergyMap = mutableMapOf<String, Float>() // YYYY-MM -> total energy

                    snapshot.children.forEach { deviceSnap ->
                        val deviceUserId = deviceSnap.child("userId").getValue(String::class.java)
                        if (deviceUserId == userId) {
                            val deviceId = deviceSnap.key ?: ""
                            val deviceName = deviceSnap.child("deviceName").getValue(String::class.java) ?: "Unknown"
                            val currentPower = deviceSnap.child("livePower").getValue(Int::class.java) ?: 0

                            // Process power history
                            val powerHistory = mutableListOf<PowerReading>()
                            deviceSnap.child("powerHistory").children.forEach { historySnap ->
                                val timestamp = historySnap.key ?: ""
                                val value = historySnap.getValue(Float::class.java) ?: 0f
                                val date = try {
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timestamp)
                                } catch (e: Exception) {
                                    null
                                }
                                powerHistory.add(PowerReading(timestamp, value, date))
                                
                                // Aggregate power readings
                                powerReadingsMap[timestamp] = (powerReadingsMap[timestamp] ?: 0f) + value
                            }

                            // Process monthly energy
                            val monthlyEnergyHistory = mutableListOf<MonthlyEnergyData>()
                            deviceSnap.child("powerMonth").children.forEach { monthSnap ->
                                val month = monthSnap.key ?: ""
                                val energy = monthSnap.getValue(Float::class.java) ?: 0f
                                val date = try {
                                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(month)
                                } catch (e: Exception) {
                                    null
                                }
                                monthlyEnergyHistory.add(MonthlyEnergyData(month, energy, date))
                                
                                // Aggregate monthly energy
                                monthlyEnergyMap[month] = (monthlyEnergyMap[month] ?: 0f) + energy
                            }

                            devices.add(DeviceConsumption(
                                deviceId = deviceId,
                                deviceName = deviceName,
                                currentPower = currentPower,
                                monthlyEnergy = monthlyEnergyHistory.lastOrNull()?.totalEnergy ?: 0f,
                                powerHistory = powerHistory,
                                monthlyEnergyHistory = monthlyEnergyHistory
                            ))
                            currentPowerSum += currentPower
                            monthlyEnergySum += monthlyEnergyHistory.lastOrNull()?.totalEnergy ?: 0f
                        }
                    }

                    // Convert aggregated data to lists
                    combinedPowerReadings = powerReadingsMap.map { (timestamp, value) ->
                        val date = try {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timestamp)
                        } catch (e: Exception) {
                            null
                        }
                        PowerReading(timestamp, value, date)
                    }.sortedBy { it.date }

                    // Calculate averages
                    calculateAverages(combinedPowerReadings)

                    combinedMonthlyEnergy = monthlyEnergyMap.map { (month, energy) ->
                        val date = try {
                            SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(month)
                        } catch (e: Exception) {
                            null
                        }
                        MonthlyEnergyData(month, energy, date)
                    }.sortedBy { it.date }

                    devicesList = devices.sortedByDescending { it.currentPower }
                    totalCurrentPower = currentPowerSum
                    totalMonthlyEnergy = monthlyEnergySum
                    isLoading = false
                    errorMessage = null

                } catch (e: Exception) {
                    errorMessage = "Error loading data: ${e.message}"
                    isLoading = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                errorMessage = "Database error: ${error.message}"
                isLoading = false
            }
        }

        devicesRef.addValueEventListener(listener)

        onDispose {
            devicesRef.removeEventListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Energy Analytics",
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5A7A7A)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF5A7A7A))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                devicesList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No devices found. Add a device to start monitoring energy consumption.",
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Total Consumption Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Total Consumption",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Current Power",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "$totalCurrentPower W",
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Monthly Energy",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${"%.2f".format(totalMonthlyEnergy)} kWh",
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Averages Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Consumption Averages",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (combinedPowerReadings.isEmpty()) {
                                        Text(
                                            text = "Not enough data to calculate averages",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Weekly Average",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${"%.2f".format(weeklyAverage)} kWh",
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Monthly Average",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${"%.2f".format(monthlyAverage)} kWh",
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Power History Graph
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Combined Power History",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp)
                                    ) {
                                        UsageGraph(
                                            readings = combinedPowerReadings.takeLast(7),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Monthly Energy Graph
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Combined Monthly Energy",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp)
                                    ) {
                                        MonthlyEnergyGraph(
                                            readings = combinedMonthlyEnergy.takeLast(12),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Devices List Header
                        item {
                            Text(
                                text = "Devices",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Individual Device Cards
                        items(devicesList.size) { index ->
                            val device = devicesList[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = device.deviceName,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${device.currentPower} W",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = "${"%.2f".format(device.monthlyEnergy)} kWh",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 
package com.example.greenmeter.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Size
import com.example.greenmeter.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Data classes for your device structure
data class Device(
    val deviceLogo: String = "",
    val deviceName: String = "",
    val livePower: Int = 0,
    val powerHistory: List<PowerReading> = emptyList(),
    val monthlyEnergy: List<MonthlyEnergyData> = emptyList(),
    val lightIntensity: Int = 0,
    val lightMode: String = "manual", // Can be "manual", "auto"
    val userId: String = ""
)

data class PowerReading(
    val timestamp: String = "",
    val value: Float = 0f,
    val date: Date? = null
)

data class MonthlyEnergyData(
    val month: String = "", // Format: "YYYY-MM"
    val totalEnergy: Float = 0f, // Total energy in kWh
    val date: Date? = null
)

// Get the last 7 values from the database (most recent 7 readings)
fun getLastSevenValues(powerHistory: List<PowerReading>): List<PowerReading> {
    // Sort by date and take the last 7 readings
    return powerHistory.sortedBy { it.date ?: Date(0) }.takeLast(7)
}

// Calculate average of the last 7 values
fun calculateAverageOfLastSeven(readings: List<PowerReading>): Float {
    return if (readings.isNotEmpty()) {
        readings.map { it.value }.average().toFloat()
    } else {
        0f
    }
}

// Helper function to get time labels for the graph
fun getTimeLabelsForReadings(readings: List<PowerReading>): List<String> {
    if (readings.isEmpty()) return emptyList()

    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return readings.map { reading ->
        reading.date?.let { dateFormat.format(it) } ?: reading.timestamp
    }
}

// Get the last 12 months of energy data
fun getLastTwelveMonths(monthlyEnergy: List<MonthlyEnergyData>): List<MonthlyEnergyData> {
    // Group by month and sum the values for each month
    val monthlyMap = mutableMapOf<String, Pair<Float, Date?>>()
    
    monthlyEnergy.forEach { data ->
        val monthKey = data.month // This is already in YYYY-MM format
        val existingData = monthlyMap[monthKey]
        if (existingData != null) {
            // Sum the energy values for the same month
            monthlyMap[monthKey] = Pair(
                existingData.first + data.totalEnergy,
                existingData.second ?: data.date
            )
        } else {
            monthlyMap[monthKey] = Pair(data.totalEnergy, data.date)
        }
    }
    
    // Convert back to list and sort by date, then take last 12
    return monthlyMap.map { (month, data) ->
        MonthlyEnergyData(month, data.first, data.second)
    }.sortedBy { it.date ?: Date(0) }.takeLast(12)
}

// Calculate average monthly energy consumption
fun calculateAverageMonthlyEnergy(readings: List<MonthlyEnergyData>): Float {
    return if (readings.isNotEmpty()) {
        readings.map { it.totalEnergy }.average().toFloat()
    } else {
        0f
    }
}

// Helper function to get month labels for the graph
fun getMonthLabelsForReadings(readings: List<MonthlyEnergyData>): List<String> {
    if (readings.isEmpty()) return emptyList()
    
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return readings.map { reading ->
        reading.date?.let { dateFormat.format(it) } ?: reading.month
    }
}

@Composable
fun UsageGraph(readings: List<PowerReading>, modifier: Modifier = Modifier) {
    if (readings.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not enough data to display graph",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    
    // Extract values for Y-axis calculation
    val dataPoints = readings.map { it.value }
    val minValue = dataPoints.minOrNull() ?: 0f
    val maxValue = dataPoints.maxOrNull() ?: 0f

    // Add padding to min/max for better visualization
    val padding = if (maxValue > minValue) (maxValue - minValue) * 0.1f else maxValue * 0.1f
    val yAxisMin = (minValue - padding).coerceAtLeast(0f)
    val yAxisMax = maxValue + padding

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val leftPadding = 60f  // Space for Y-axis labels
        val rightPadding = 20f
        val topPadding = 40f   // Increased for value labels
        val bottomPadding = 60f // Increased for day labels

        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - topPadding - bottomPadding
        val yAxisRange = yAxisMax - yAxisMin

        // Draw Y-axis line
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, height - bottomPadding),
            strokeWidth = 2f
        )

        // Draw X-axis line
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(leftPadding, height - bottomPadding),
            end = Offset(width - rightPadding, height - bottomPadding),
            strokeWidth = 2f
        )

        // Draw horizontal grid lines and Y-axis labels
        val numYTicks = 5
        for (i in 0..numYTicks) {
            val y = topPadding + (graphHeight * i / numYTicks)
            val value = yAxisMax - (yAxisRange * i / numYTicks)

            // Grid line
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(leftPadding, y),
                end = Offset(width - rightPadding, y),
                strokeWidth = 1f
            )

            // Y-axis label
            val labelText = "${value.toInt()} kWh"
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp
                ),
                topLeft = Offset(
                    x = leftPadding - 45f,
                    y = y - 8f
                )
            )
        }

        // Draw vertical grid lines
        val numXTicks = readings.size - 1
        if (numXTicks > 0) {
            for (i in 0..numXTicks) {
                val x = leftPadding + (graphWidth * i / numXTicks)
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(x, topPadding),
                    end = Offset(x, height - bottomPadding),
                    strokeWidth = 1f
                )
            }
        }

        // Draw the data line and points
        if (readings.isNotEmpty() && readings.size > 1) {
            val path = Path()
            val stepX = graphWidth / (readings.size - 1)

            // Create path for line
            readings.forEachIndexed { index, reading ->
                val x = leftPadding + stepX * index
                val normalizedValue = ((reading.value - yAxisMin) / yAxisRange).coerceIn(0f, 1f)
                val y = height - bottomPadding - (normalizedValue * graphHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw the line with gradient
            drawPath(
                path = path,
                color = Color(0xFF4CAF50),
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Add subtle gradient under the line
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width - rightPadding, height - bottomPadding)
                lineTo(leftPadding, height - bottomPadding)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4CAF50).copy(alpha = 0.2f),
                        Color(0xFF4CAF50).copy(alpha = 0.0f)
                    ),
                    startY = topPadding,
                    endY = height - bottomPadding
                )
            )

            // Draw points and values
            readings.forEachIndexed { index, reading ->
                val x = leftPadding + stepX * index
                val normalizedValue = ((reading.value - yAxisMin) / yAxisRange).coerceIn(0f, 1f)
                val y = height - bottomPadding - (normalizedValue * graphHeight)

                // Draw point
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 6f,
                    center = Offset(x, y)
                )

                // Draw inner point
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, y)
                )

                // Draw value above point
                val valueText = "${reading.value.toInt()} kWh"
                val textWidth = textMeasurer.measure(valueText).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = valueText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    topLeft = Offset(
                        x = x - textWidth / 2,
                        y = y - 25f // Position above the point
                    )
                )

                // Draw day of week below point
                val dayText = reading.date?.let { date ->
                    SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                } ?: ""
                val dayWidth = textMeasurer.measure(dayText).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = dayText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    topLeft = Offset(
                        x = x - dayWidth / 2,
                        y = height - bottomPadding + 10f
                    )
                )
            }
        }
    }
}

@Composable
fun MonthlyEnergyGraph(readings: List<MonthlyEnergyData>, modifier: Modifier = Modifier) {
    if (readings.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not enough data to display graph",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    
    // Extract values for Y-axis calculation
    val dataPoints = readings.map { it.totalEnergy }
    val minValue = dataPoints.minOrNull() ?: 0f
    val maxValue = dataPoints.maxOrNull() ?: 0f

    // Add padding to min/max for better visualization
    val padding = if (maxValue > minValue) (maxValue - minValue) * 0.1f else maxValue * 0.1f
    val yAxisMin = (minValue - padding).coerceAtLeast(0f)
    val yAxisMax = maxValue + padding
    val yAxisRange = yAxisMax - yAxisMin

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val leftPadding = 60f  // Space for Y-axis labels
        val rightPadding = 20f
        val topPadding = 20f
        val bottomPadding = 60f // Increased for month labels

        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - topPadding - bottomPadding

        // Draw axes
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, height - bottomPadding),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(leftPadding, height - bottomPadding),
            end = Offset(width - rightPadding, height - bottomPadding),
            strokeWidth = 2f
        )

        // Draw horizontal grid lines and Y-axis labels
        val numYTicks = 5
        for (i in 0..numYTicks) {
            val y = topPadding + (graphHeight * i / numYTicks)
            val value = yAxisMax - (yAxisRange * i / numYTicks)

            // Grid line
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(leftPadding, y),
                end = Offset(width - rightPadding, y),
                strokeWidth = 1f
            )

            // Y-axis label
            val labelText = "${value.toInt()} kWh"
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp
                ),
                topLeft = Offset(
                    x = leftPadding - 45f,
                    y = y - 8f
                )
            )
        }

        // Draw bars with gradient
        if (dataPoints.isNotEmpty()) {
            val barWidth = (graphWidth / dataPoints.size) * 0.6f
            val barSpacing = graphWidth / dataPoints.size

            dataPoints.forEachIndexed { index, value ->
                val x = leftPadding + (index * barSpacing) + (barSpacing - barWidth) / 2
                val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
                val barHeight = normalizedValue * graphHeight

                // Draw bar with gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50),
                            Color(0xFF2E7D32)
                        ),
                        startY = height - bottomPadding - barHeight,
                        endY = height - bottomPadding
                    ),
                    topLeft = Offset(
                        x = x,
                        y = height - bottomPadding - barHeight
                    ),
                    size = Size(
                        width = barWidth,
                        height = barHeight
                    )
                )

                // Draw bar highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(x, height - bottomPadding - barHeight),
                    end = Offset(x + barWidth, height - bottomPadding - barHeight),
                    strokeWidth = 2f
                )

                // Draw value on top of the bar
                val valueText = "${value.toInt()} kWh"
                val textWidth = textMeasurer.measure(valueText).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = valueText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    topLeft = Offset(
                        x = x + (barWidth - textWidth) / 2,
                        y = height - bottomPadding - barHeight - 20f
                    )
                )
            }
        }

        // Draw month labels with correct positioning
        if (readings.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val barSpacing = graphWidth / readings.size

            readings.forEachIndexed { index, reading ->
                val x = leftPadding + (index * barSpacing) + barSpacing / 2
                val monthText = reading.date?.let { date ->
                    dateFormat.format(date)
                } ?: reading.month.substring(5) // Extract month part from YYYY-MM

                val textWidth = textMeasurer.measure(monthText).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = monthText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp
                    ),
                    topLeft = Offset(
                        x = x - textWidth / 2,
                        y = height - bottomPadding + 10f
                    )
                )
            }
        }
    }
}

@Composable
fun DeviceDetails(deviceId: String, navController: NavController) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Function to delete device
    fun deleteDevice() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val deviceRef = database.getReference("devices").child(deviceId)
            
            deviceRef.removeValue()
                .addOnSuccessListener {
                    // Navigate back after successful deletion
                    navController.popBackStack()
                }
                .addOnFailureListener { e ->
                    // Handle error (you might want to show a toast or error message)
                    println("Error deleting device: ${e.message}")
                }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Device") },
            text = { Text("Are you sure you want to delete this device? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDevice()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Get current user ID from Firebase Auth
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    // Use the Flow-based approach for cleaner Firebase integration
    val deviceFlow = remember(deviceId) {
        callbackFlow {
            val database = FirebaseDatabase.getInstance()
            val deviceRef = database.getReference("devices").child(deviceId)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Check if the snapshot exists and belongs to the current user
                        if (!snapshot.exists()) {
                            trySend(null)
                            return
                        }

                        val deviceUserId = snapshot.child("userId").getValue(String::class.java)
                        if (deviceUserId != userId) {
                            trySend(null)
                            return
                        }

                        // Get device data
                        val power = snapshot.child("livePower").getValue(Int::class.java) ?: 0
                        val deviceName = snapshot.child("deviceName").getValue(String::class.java) ?: "Unknown Device"
                        val deviceLogo = snapshot.child("deviceLogo").getValue(String::class.java) ?: ""
                        val lightIntensity = snapshot.child("lightIntensity").getValue(Int::class.java) ?: 0
                        val lightMode = snapshot.child("lightMode").getValue(String::class.java) ?: "manual"

                        // Get power history with proper parsing and date conversion
                        val powerHistory = mutableListOf<PowerReading>()
                        val historySnapshot = snapshot.child("powerHistory")

                        if (historySnapshot.exists()) {
                            historySnapshot.children.forEach { historyChild ->
                                val timestamp = historyChild.key ?: ""
                                val value = when (val rawValue = historyChild.value) {
                                    is Number -> rawValue.toFloat()
                                    is String -> rawValue.toFloatOrNull() ?: 0f
                                    else -> 0f
                                }

                                // Parse timestamp to Date object with better error handling
                                val date = try {
                                    when {
                                        // Handle ISO timestamp format: "2025-05-25T14:25"
                                        timestamp.contains("T") -> {
                                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timestamp)
                                        }
                                        // Handle date format: "2025-05-25"
                                        timestamp.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(timestamp)
                                        }
                                        // Handle timestamp as milliseconds
                                        timestamp.all { it.isDigit() } && timestamp.length > 10 -> {
                                            Date(timestamp.toLong())
                                        }
                                        // Try to parse as epoch seconds
                                        timestamp.all { it.isDigit() } && timestamp.length <= 10 -> {
                                            Date(timestamp.toLong() * 1000)
                                        }
                                        // Current time if we can't parse (for new entries)
                                        else -> {
                                            println("Using current time for unparseable timestamp: $timestamp")
                                            Date()
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("Error parsing timestamp: $timestamp - ${e.message}")
                                    // Use current time as fallback for new entries
                                    Date()
                                }

                                powerHistory.add(PowerReading(timestamp, value, date))
                            }
                        }

                        // Get monthly energy data
                        val monthlyEnergyMap = mutableMapOf<String, MutableList<Float>>()
                        val monthlySnapshot = snapshot.child("powerMonth")

                        if (monthlySnapshot.exists()) {
                            monthlySnapshot.children.forEach { monthChild ->
                                val monthKey = monthChild.key ?: "" // This will be in YYYY-MM format
                                val value = when (val rawValue = monthChild.value) {
                                    is Number -> rawValue.toFloat()
                                    is String -> rawValue.toFloatOrNull() ?: 0f
                                    else -> 0f
                                }

                                // Parse the month key directly as the date
                                val date = try {
                                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey)
                                } catch (e: Exception) {
                                    println("Error parsing month key: $monthKey - ${e.message}")
                                    null
                                }

                                // Add to monthly energy data
                                if (!monthlyEnergyMap.containsKey(monthKey)) {
                                    monthlyEnergyMap[monthKey] = mutableListOf()
                                }
                                monthlyEnergyMap[monthKey]?.add(value)
                            }
                        }

                        // Convert grouped data to MonthlyEnergyData objects
                        val monthlyEnergy = monthlyEnergyMap.map { (monthKey, values) ->
                            val totalEnergy = values.sum()
                            val date = try {
                                SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey)
                            } catch (e: Exception) {
                                null
                            }
                            MonthlyEnergyData(monthKey, totalEnergy, date)
                        }.toMutableList()

                        // Sort power history and monthly energy by date
                        powerHistory.sortBy { it.date ?: Date(0) }
                        monthlyEnergy.sortBy { it.date ?: Date(0) }

                        val device = Device(
                            deviceLogo = deviceLogo,
                            deviceName = deviceName,
                            livePower = power,
                            powerHistory = powerHistory,
                            monthlyEnergy = monthlyEnergy,
                            lightIntensity = lightIntensity,
                            lightMode = lightMode,
                            userId = deviceUserId ?: ""
                        )

                        trySend(device)

                    } catch (e: Exception) {
                        println("Error parsing device data: ${e.message}")
                        trySend(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Firebase error: ${error.message}")
                    close(error.toException())
                }
            }

            deviceRef.addValueEventListener(listener)

            awaitClose {
                deviceRef.removeEventListener(listener)
            }
        }
    }

    // Collect the device state
    val device by deviceFlow.collectAsState(initial = null)
    var isLoading by remember { mutableStateOf(true) }

    // Update loading state when device data changes
    LaunchedEffect(device) {
        isLoading = false
    }

    when {
        isLoading -> {
            // Loading state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF5A7A7A)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Loading device data...",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        device == null -> {
            // Error/Not found state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF5A7A7A)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (userId == null) {
                    Text(
                        text = "User not authenticated",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = "Device not found or error loading data",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Path: devices/$deviceId",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        else -> {
            // Success state - show device data
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF5A7A7A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Info Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left side with logo and name
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Device Logo
                                val logoResId = when (device!!.deviceLogo) {
                                    "bedroom" -> R.drawable.bedroom
                                    "kitchen" -> R.drawable.kitchen
                                    "living_room" -> R.drawable.livingroom
                                    "bathroom" -> R.drawable.bathroom
                                    else -> R.drawable.question_mark
                                }
                                Image(
                                    painter = painterResource(id = logoResId),
                                    contentDescription = "Device Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(end = 8.dp)
                                )

                                Text(
                                    text = device!!.deviceName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Device",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Live Power: ${device!!.livePower} kWh",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Light Control Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Light Control",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Light Mode Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val buttonModifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                            val selectedMode = device!!.lightMode
                            
                            OutlinedButton(
                                onClick = {
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {
                                        val updates = hashMapOf<String, Any>(
                                            "lightMode" to "manual",
                                            "lightIntensity" to 255
                                        )
                                        FirebaseDatabase.getInstance()
                                            .getReference("devices")
                                            .child(deviceId)
                                            .updateChildren(updates)
                                    }
                                },
                                modifier = buttonModifier,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedMode == "manual") Color(0xFF4CAF50) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text("Max", fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {
                                        val updates = hashMapOf<String, Any>(
                                            "lightMode" to "manual",
                                            "lightIntensity" to 0
                                        )
                                        FirebaseDatabase.getInstance()
                                            .getReference("devices")
                                            .child(deviceId)
                                            .updateChildren(updates)
                                    }
                                },
                                modifier = buttonModifier,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedMode == "manual") Color(0xFF4CAF50) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text("Min", fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {
                                        FirebaseDatabase.getInstance()
                                            .getReference("devices")
                                            .child(deviceId)
                                            .child("lightMode")
                                            .setValue("auto")
                                    }
                                },
                                modifier = buttonModifier,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedMode == "auto") Color(0xFF4CAF50) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text("Auto", fontSize = 14.sp)
                            }
                        }

                        // Light Intensity Display
                        Text(
                            text = "Intensity: ${device!!.lightIntensity}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        var sliderPosition by remember(device!!.lightIntensity) { 
                            mutableStateOf(device!!.lightIntensity.toFloat()) 
                        }
                        
                        // Enhanced Slider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Slider(
                                value = sliderPosition,
                                onValueChange = { newValue ->
                                    if (device!!.lightMode == "manual") {
                                        sliderPosition = newValue
                                        val intValue = newValue.toInt()
                                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                                        if (userId != null) {
                                            FirebaseDatabase.getInstance()
                                                .getReference("devices")
                                                .child(deviceId)
                                                .child("lightIntensity")
                                                .setValue(intValue)
                                        }
                                    }
                                },
                                valueRange = 0f..255f,
                                steps = 254,
                                enabled = device!!.lightMode == "manual",
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4CAF50),
                                    activeTrackColor = Color(0xFF4CAF50),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                    disabledThumbColor = Color.Gray,
                                    disabledActiveTrackColor = Color.Gray,
                                    disabledInactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Mode Indicator
                        Text(
                            text = "Mode: ${if (device!!.lightMode == "auto") "Automatic" else "Manual"} Control",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Power History Section
                item {
                    val lastSevenReadings = getLastSevenValues(device!!.powerHistory)
                    val averagePower = calculateAverageOfLastSeven(lastSevenReadings)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        // Power History Header with Average
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Power History",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "7-Day Average",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${"%.1f".format(averagePower)} W",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Power History Graph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            UsageGraph(
                                readings = lastSevenReadings,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Monthly Energy Section
                item {
                    val lastTwelveMonths = getLastTwelveMonths(device!!.monthlyEnergy)
                    val averageMonthlyEnergy = calculateAverageMonthlyEnergy(lastTwelveMonths)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        // Monthly Energy Header with Average
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Energy",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Monthly Average",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${"%.1f".format(averageMonthlyEnergy)} kWh",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Monthly Energy Graph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            MonthlyEnergyGraph(
                                readings = lastTwelveMonths,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Add some padding at the bottom
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
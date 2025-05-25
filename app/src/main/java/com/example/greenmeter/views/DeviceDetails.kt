package com.example.greenmeter.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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

// Data classes for your device structure
data class Device(
    val deviceLogo: String = "",
    val deviceName: String = "",
    val livePower: Int = 0,
    val powerHistory: List<PowerReading> = emptyList()
)

data class PowerReading(
    val timestamp: String = "",
    val value: Float = 0f,
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

@Composable
fun UsageGraph(readings: List<PowerReading>, modifier: Modifier = Modifier) {
    // Use actual readings or fallback data
    val actualReadings = if (readings.isNotEmpty()) readings else listOf(
        PowerReading("", 100f, Date()),
        PowerReading("", 120f, Date()),
        PowerReading("", 180f, Date()),
        PowerReading("", 100f, Date()),
        PowerReading("", 80f, Date()),
        PowerReading("", 90f, Date()),
        PowerReading("", 150f, Date())
    )

    // Extract values for Y-axis calculation
    val dataPoints = actualReadings.map { it.value }

    // Calculate proper Y-axis range
    val minValue = dataPoints.minOrNull() ?: 0f
    val maxValue = dataPoints.maxOrNull() ?: 250f

    // Add padding to min/max for better visualization
    val padding = (maxValue - minValue) * 0.1f
    val yAxisMin = (minValue - padding).coerceAtLeast(0f)
    val yAxisMax = maxValue + padding

    Canvas(modifier = modifier) {
        drawUsageGraphWithAxis(dataPoints, yAxisMin, yAxisMax)
    }
}

fun DrawScope.drawUsageGraphWithAxis(dataPoints: List<Float>, yAxisMin: Float, yAxisMax: Float) {
    val width = size.width
    val height = size.height
    val leftPadding = 60f  // Space for Y-axis labels
    val rightPadding = 20f
    val topPadding = 20f
    val bottomPadding = 40f

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

        // Y-axis tick mark
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = Offset(leftPadding - 5f, y),
            end = Offset(leftPadding, y),
            strokeWidth = 2f
        )
    }

    // Draw vertical grid lines
    val numXTicks = dataPoints.size - 1
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
    if (dataPoints.isNotEmpty() && dataPoints.size > 1) {
        val path = Path()
        val stepX = if (dataPoints.size > 1) graphWidth / (dataPoints.size - 1) else graphWidth

        // Create path for line
        dataPoints.forEachIndexed { index, value ->
            val x = leftPadding + stepX * index
            val normalizedValue = ((value - yAxisMin) / yAxisRange).coerceIn(0f, 1f)
            val y = height - bottomPadding - (normalizedValue * graphHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw the line with gradient effect
        drawPath(
            path = path,
            color = Color(0xFF4CAF50),
            style = Stroke(width = 4f)
        )

        // Draw points with values
        dataPoints.forEachIndexed { index, value ->
            val x = leftPadding + stepX * index
            val normalizedValue = ((value - yAxisMin) / yAxisRange).coerceIn(0f, 1f)
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
        }
    }
}

// Keep the old function for backward compatibility but make it use the new one
fun DrawScope.drawUsageGraph(dataPoints: List<Float>, maxValue: Float) {
    val minValue = dataPoints.minOrNull() ?: 0f
    drawUsageGraphWithAxis(dataPoints, minValue, maxValue)
}

@Composable
fun DeviceDetails(deviceId: String, navController: NavController) {
    // Get current user ID from Firebase Auth
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    // Use the Flow-based approach for cleaner Firebase integration
    val deviceFlow = remember(deviceId, userId) {
        callbackFlow {
            if (userId == null) {
                trySend(null)
                return@callbackFlow
            }

            val database = FirebaseDatabase.getInstance()
            // Updated path to match your Firebase structure: users/[userId]/[deviceId]
            val deviceRef = database.getReference("users").child(userId).child(deviceId)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Check if the snapshot exists
                        if (!snapshot.exists()) {
                            trySend(null)
                            return
                        }

                        // Get live power with proper null handling
                        val power = snapshot.child("livePower").getValue(Int::class.java) ?: 0
                        val deviceName = snapshot.child("deviceName").getValue(String::class.java) ?: "Unknown Device"
                        val deviceLogo = snapshot.child("deviceLogo").getValue(String::class.java) ?: ""

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

                        // Sort power history by date/timestamp
                        powerHistory.sortBy { it.date ?: Date(0) }

                        val device = Device(
                            deviceLogo = deviceLogo,
                            deviceName = deviceName,
                            livePower = power,
                            powerHistory = powerHistory
                        )

                        trySend(device)

                    } catch (e: Exception) {
                        // Log the error and send null
                        println("Error parsing device data: ${e.message}")
                        trySend(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Firebase error: ${error.message}")
                    close(error.toException())
                }
            }

            // Add the listener
            deviceRef.addValueEventListener(listener)

            // Cleanup when the flow is cancelled
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
                        text = "Path: users/$userId/$deviceId",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        else -> {
            // Success state - show device data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF5A7A7A))
                    .padding(16.dp)
            ) {
                // Device Info Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Device: ${device!!.deviceName}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (device!!.deviceLogo.isNotEmpty()) {
                        Text(
                            text = "Logo: ${device!!.deviceLogo}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Live Power: ${device!!.livePower} W",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Get the last 7 values from the database
                val lastSevenReadings = getLastSevenValues(device!!.powerHistory)
                val averagePower = calculateAverageOfLastSeven(lastSevenReadings)
                val timeLabels = getTimeLabelsForReadings(lastSevenReadings)

                // Calculate Y-axis values for display
                val dataValues = lastSevenReadings.map { it.value }
                val minValue = if (dataValues.any { it > 0 }) dataValues.filter { it > 0 }.minOrNull() ?: 0f else 0f
                val maxValue = dataValues.maxOrNull() ?: 100f

                // Power History Header with Average
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Power History (Last 7 Values)",
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

                // Graph container with Y-axis labels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    // Y-axis labels (positioned on the left)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(50.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val yAxisRange = maxValue - minValue
                        for (i in 5 downTo 0) {
                            val value = minValue + (yAxisRange * i / 5)
                            Text(
                                text = "${value.toInt()}W",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    // Graph with last 7 values
                    UsageGraph(
                        readings = lastSevenReadings,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show time range for the last 7 values
                if (lastSevenReadings.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = timeLabels.firstOrNull() ?: "",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                        if (timeLabels.size > 1) {
                            Text(
                                text = timeLabels.lastOrNull() ?: "",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced debug info
                Text(
                    text = "Total readings: ${device!!.powerHistory.size} | Showing last 7 values | Average: ${"%.1f".format(averagePower)}W",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Show value range and actual data points
                Text(
                    text = "Value range: ${minValue.toInt()}-${maxValue.toInt()}W | Last 7 readings from database",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                // Show the last 7 values for debugging
                if (lastSevenReadings.isNotEmpty()) {
                    Text(
                        text = "Last 7 values: ${lastSevenReadings.map { "${it.value.toInt()}W" }.joinToString(", ")}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
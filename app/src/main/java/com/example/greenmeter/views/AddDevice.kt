package com.example.greenmeter.views

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.greenmeter.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDevice(navController: NavController) {
    var deviceName by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var selectedLogo by remember { mutableStateOf("") }
    val context = LocalContext.current

    val logoOptions = listOf(
        "bedroom" to R.drawable.bedroom,
        "kitchen" to R.drawable.kitchen,
        "living_room" to R.drawable.livingroom,
        "bathroom" to R.drawable.bathroom
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5A7A7A)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add New Device",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Device Name", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = deviceId,
                onValueChange = { deviceId = it },
                label = { Text("Device ID", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Text(
                text = "Select Device Logo",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logoOptions) { (logo, drawableId) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedLogo = logo },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLogo == logo) 
                                Color(0xFF7A9292)
                            else 
                                Color(0xFF6B8989)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(drawableId),
                                contentDescription = logo,
                                modifier = Modifier
                                    .size(60.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (deviceName.isBlank() || deviceId.isBlank() || selectedLogo.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val userId = Firebase.auth.currentUser?.uid
                if (userId != null) {
                    val database = FirebaseDatabase.getInstance()
                    val deviceRef = database.getReference("devices").child(deviceId)
                    
                    // Get the current device data first
                    deviceRef.get().addOnSuccessListener { snapshot ->
                        val currentData = if (snapshot.exists()) {
                            snapshot.value as? Map<*, *> ?: mapOf<String, Any>()
                        } else {
                            mapOf<String, Any>()
                        }

                        // Prepare the update data, preserving existing values if present
                        val deviceData = mapOf(
                            "deviceName" to deviceName,
                            "deviceLogo" to selectedLogo,
                            "userId" to userId,
                            "livePower" to (currentData["livePower"] as? Int ?: 0),
                            "lightIntensity" to (currentData["lightIntensity"] as? Int ?: 0),
                            "lightMode" to (currentData["lightMode"] as? String ?: "manual"),
                            "powerHistory" to (currentData["powerHistory"] as? Map<*, *> ?: mapOf<String, Int>()),
                            "powerMonth" to (currentData["powerMonth"] as? Map<*, *> ?: mapOf<String, Int>())
                        )

                        // Update or create the device
                        deviceRef.setValue(deviceData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context, 
                                    if (snapshot.exists()) "Device updated successfully" else "Device added successfully", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context, 
                                    if (snapshot.exists()) "Failed to update device" else "Failed to add device", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to access device data", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2D3F3F)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Add Device",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
} 
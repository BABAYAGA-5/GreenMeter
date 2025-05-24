package com.example.location

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.FirebaseDatabase

@Composable
fun HomeScreen(navController: NavController) {
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
                val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
                logoutButton.setOnClickListener {
                    Log.d("HomeScreen", "Logout clicked")
                    Firebase.auth.signOut()
                    navController.navigate("login")
                }
                val greetingText = view.findViewById<TextView>(R.id.greetingText)
                val datetime = view.findViewById<TextView>(R.id.datetime)
                val curentdatetime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(java.util.Date())
                datetime.text = curentdatetime
                val currenttime = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
                when (currenttime) {
                    in "00:00:00".."06:00:00" -> greetingText.text = "Good Night"
                    in "06:00:01".."12:00:00" -> greetingText.text = "Good Morning"
                    in "12:00:01".."18:00:00" -> greetingText.text = "Good Afternoon"
                    in "18:00:01".."23:59:59" -> greetingText.text = "Good Evening"
                }
                val displayName = view.findViewById<TextView>(R.id.displayName)
                displayName.text = "${Firebase.auth.currentUser?.displayName}"

                val devicesContainer = view.findViewById<GridLayout>(R.id.devicesContainer)
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

                    val labelLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(16, 16, 16, 8)
                        }
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val roomsLabel = TextView(context).apply {
                        text = "Rooms"
                        textSize = 20f
                        setTextColor(ContextCompat.getColor(context, R.color.black))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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
                            // Handle adding a new device
                            navController.navigate("addDevice")
                        } else {
                            Log.d("HomeScreen", "Navigating to device details for: ${device?.deviceId}")
                            // Handle navigating to device details
                            navController.navigate("deviceDetails/${device?.deviceId}")
                        }
                    }
                }

                val devicesList = mutableListOf<Device>()
                val db = FirebaseDatabase.getInstance()
                val userId = Firebase.auth.currentUser?.uid ?: ""
                val userRef = db.getReference("users/$userId")
                var snapshotexists: Boolean? = false

                Log.d("Firebase", "Fetching devices for user: $userId")
                userRef.get().addOnSuccessListener { snapshot ->
                    devicesList.clear()
                    snapshotexists = snapshot.exists()
                    if (snapshot.exists()) {
                        Log.d("Firebase", "User $userId exists, fetching devices")
                        for (deviceSnap in snapshot.children) {
                            val deviceId = deviceSnap.key ?: ""
                            val deviceName = deviceSnap.child("deviceName").getValue(String::class.java) ?: "unknown"
                            val deviceLogo = deviceSnap.child("deviceLogo").getValue(String::class.java) ?: "unknown"
                            devicesList.add(Device(deviceId, deviceName, deviceLogo))
                        }
                        for (device in devicesList) {
                            Log.d("Firebase", "Device found: ${device.deviceName} with ID: ${device.deviceId} and logo: ${device.deviceLogo}")
                            addCard(device)
                        }
                        Log.d("Firebase", "Total devices found: ${devicesList.size}")
                        val addDevice: Device = Device("new", "New device", "add")
                        addCard(addDevice)
                        /*val seeAllButton = Button(context).apply {
                            text = "See all"
                            textSize = 16f
                            setPadding(32, 16, 32, 16)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                gravity = Gravity.CENTER
                                topMargin = 16
                                bottomMargin = 32
                            }
                            setOnClickListener {
                                Log.d("HomeScreen", "See all button clicked")
                                // Handle click
                            }
                        }
                        contentContainer.addView(seeAllButton)*/
                    }
                    else {
                        Log.d("Firebase", "No devices found for user $userId")
                        Log.d("Firebase", "Creating new user entry")
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "Failed to get devices", it)
                }










                val profileButton = view.findViewById<ImageButton>(R.id.profileButton)
                val homeButton = view.findViewById<ImageButton>(R.id.homeButton)
                val analyticsButton = view.findViewById<ImageButton>(R.id.analyticsButton)

                profileButton.setOnClickListener {
                    Log.d("HomeScreen", "Profile clicked")
                    navController.navigate("profile")
                }

                homeButton.setOnClickListener {
                    Log.d("HomeScreen", "Home clicked")
                    // Already on Home Screen, no action needed
                }

                view
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
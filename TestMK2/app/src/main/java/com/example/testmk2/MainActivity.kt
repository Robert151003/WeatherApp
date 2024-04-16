package com.example.testmk2

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar


class MainActivity : ComponentActivity() {

    private val locationViewModel: LocationViewModel by viewModels()

    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 30
        fastestInterval = 10
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        maxWaitTime = 60
    }
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                val location = locationList.last()
                // Call method to handle location
                handleLocation(location.latitude, location.longitude)

                locationViewModel.lat = location.latitude
                locationViewModel.lon = location.longitude
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)


        // Request notification permission
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermission()
        }

        // Check for location permission before attempting to retrieve last known location
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted, attempt to retrieve last known location
            retrieveLastKnownLocation()
        } else {
            // Permission not granted, request it
            requestLocationPermission()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun retrieveLastKnownLocation() {
        try {
            fusedLocationProvider.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Latitude and longitude are available, so set them in the view model
                        locationViewModel.lat = location.latitude
                        locationViewModel.lon = location.longitude
                        // Call setContent to set up the UI with the retrieved location
                        setContent {
                            val navController = rememberNavController()

                            NavHost(navController = navController, startDestination = Routes.screenB) {
                                composable(Routes.screenA) {
                                    val webView = remember {
                                        WebView(this@MainActivity).apply {
                                            settings.javaScriptEnabled = true
                                        }
                                    }
                                    ScreenA(navController, applicationContext, this@MainActivity, locationViewModel, webView)
                                }
                                composable(Routes.screenB) {
                                    // Retrieve latitude and longitude coordinates
                                    ScreenB(navController, applicationContext, this@MainActivity, locationViewModel)
                                }
                            }
                        }
                    } ?: run {
                        // Last known location is not available, handle it as needed
                        Log.e("Location", "Last known location is null")
                    }
                }
                .addOnFailureListener { e ->
                    // Failed to retrieve last known location, handle the failure
                    Log.e("Location", "Failed to retrieve last known location: ${e.message}")
                }
        } catch (securityException: SecurityException) {
            // Handle SecurityException
            Log.e("Location", "SecurityException: ${securityException.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestLocationPermission() {
        // Request location permission
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, attempt to retrieve last known location
                retrieveLastKnownLocation()
            } else {
                // Permission denied, handle it as needed
                // For example, show a message to the user indicating that permission is required
                Log.e("Location", "Location permission denied")
            }
        }


    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProvider.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            // You can request permission here or handle it in a different way
            // For simplicity, I'll just log a message
            Log.e("Location Permission", "Location permission not granted.")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProvider.removeLocationUpdates(locationCallback)
    }

    private fun handleLocation(latitude: Double, longitude: Double): Pair<Double, Double>? {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                return Pair(location.latitude, location.longitude)
            } else {
                // Failed to get current location
                return null
            }
        } catch (e: SecurityException) {
            // Handle permission-related exceptions here
            return null
        } catch (e: Exception) {
            // Handle other exceptions here
            return null
        }
    }






    private fun requestNotificationPermission() {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                if (Build.VERSION.SDK_INT >= 33) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionRationale()
                    } else {
                        showSettingDialog()
                    }
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Notification permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun showSettingDialog() {
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3
        )
            .setTitle("Notification Permission")
            .setMessage("Notification permission is required. Please allow notification permission from settings.")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3
        )
            .setTitle("Alert")
            .setMessage("Notification permission is required to show notifications.")
            .setPositiveButton("Ok") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    requestNotificationPermission()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}


object NotificationHelper {

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(context: Context) {
        val name = "Notif Channel"
        val desc = "Description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleNotifications(context: Context, title:String, message:String) {
        val intent = Intent(context, NotificationReceiver::class.java)

        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getTime()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }

    private fun getTime(): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // If the alarm time has already passed for today, schedule it for the next day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }
}





package com.example.testmk2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale


var lat = 0.0
var lon = 0.0
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ScreenA(navController: NavController, context: Context, newsContext: Context, locationViewModel: LocationViewModel, webView: WebView) {

    val tempList = remember { mutableStateListOf<String>() }
    val timeList = remember { mutableStateListOf<String>() }
    var apiCalled by remember { mutableStateOf(false) }

    var city by remember { mutableStateOf("") } // Assuming a default city

    // Get the users coordinates
    lat = locationViewModel.lat
    lon = locationViewModel.lon

    LaunchedEffect(Unit) {

        // Check if enough time has passed to access the api, if not get from local storage
        val storedTempData = retrieveWeatherDataFromStorage(context, "weatherData")
        val storedTimeData = retrieveWeatherDataFromStorage(context, "weatherTime")
        if (fetchWeatherData(tempList, timeList, context, apiCalled)) {
            tempList.addAll(storedTempData)
            timeList.addAll(storedTimeData)
            Log.d("Data", "Loaded weather data from storage: $tempList")
        } else {
            fetchWeatherData(tempList, timeList, context, apiCalled)
        }

        for (entry in tempList) {
            Log.d("List", entry)
        }
        city = fetchCityCoordinatesA(api)

    }

    // Top Buttons
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF122259),
                        Color(0xFF9561a1)
                    )
                )
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // Align buttons at the start and end of the row
        ) {
            // Change view button (left-aligned)
            Button(
                onClick = {
                    navController.navigate(Routes.screenB)
                },
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 8.dp) // Adjust padding as needed
                    .weight(1f) // Occupy equal space as the other button
            ) {
                Text(text = "Weekly Weather")
            }

            // News button (right-aligned)
            Button(
                onClick = {
                    goToWeatherA(newsContext, lat.toString(), lon.toString())
                },
                modifier = Modifier
                    .padding(start = 8.dp, top = 16.dp, end = 16.dp) // Adjust padding as needed
                    .weight(1f) // Occupy equal space as the other button
            ) {
                Text(text = "Weather")
            }
        }

        // Weather Data
        Column(
            modifier = Modifier
                .fillMaxSize()
        ){

            // Today
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 150.dp), // Adjust top padding as needed
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text(text = city, color = Color.White, fontSize = 30.sp)
                Text(
                    text = weatherDataDisplay(tempList, 0, 0),
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(bottom = 40.dp)
                        .align(Alignment.CenterHorizontally)
                )



                LazyRow(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Assuming you have a structured data model or a predefined list
                    // For demonstration, using indexed approach with placeholders

                    itemsIndexed(timeList.take(6)) { index, time ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 24.dp) // Space between each column
                        ) {
                            Text(
                                text = time,
                                color = Color.White,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp)) // Space between time and temperature

                            val temperature = if (weatherDataDisplay(tempList, 0, index + 1) == "na") {
                                weatherDataDisplay(tempList, 1, index + 1)
                            } else {
                                weatherDataDisplay(tempList, 0, index + 1)
                            }

                            Text(
                                text = "$temperature°C",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }


                }

            }

            // Future

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 64.dp), // Adjust top padding as needed
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ){

                // Day 1
                Row(modifier = Modifier
                    .padding(10.dp)){
                    Text(
                        text = weatherDataDisplay(tempList, 1, 0) + "   →",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Column{
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunrise_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 1, 2) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.midday_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 1, 4) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)

                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunset_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 1, 6) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Day 2
                Row(modifier = Modifier
                    .padding(10.dp)){
                    Text(
                        text = weatherDataDisplay(tempList, 2, 0) + "   →",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Column{
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunrise_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 2, 2) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.midday_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 2, 4) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)

                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunset_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 2, 6) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Day 3
                Row(modifier = Modifier
                    .padding(10.dp)){
                    Text(
                        text = weatherDataDisplay(tempList, 3, 0) + "   →",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Column{
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunrise_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 3, 2) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.midday_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 3, 4) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)

                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunset_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 3, 6) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Day 4
                Row(modifier = Modifier
                    .padding(10.dp)){
                    Text(
                        text = weatherDataDisplay(tempList, 4, 0) + "   →",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Column{
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunrise_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 4, 2) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.midday_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .padding(end = 13.dp)
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 4, 4) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 10.dp)

                                )
                            }
                            Column{
                                Image(
                                    painter = painterResource(id = R.drawable.sunset_icon),
                                    contentDescription = "Morning",
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 30.dp)
                                )
                                Text(
                                    text = weatherDataDisplay(tempList, 4, 6) + "°C",
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

    }


}


// Split the tempList to get the specific data
@Composable
fun weatherDataDisplay(tempList: MutableList<String>, index1: Int, index2: Int): String {
    val data = if (index1 < tempList.size) {
        val parts = tempList[index1].split(", ")
        if (index2 < parts.size) {
            parts[index2]
        } else {
            "na"
        }
    } else {
        "na"
    }
    return data
}


// Access the api to get the weather data
@OptIn(DelicateCoroutinesApi::class)
fun fetchWeatherData(tempList: MutableList<String>, timeList: MutableList<String>, context: Context, apiCalled: Boolean): Boolean {
    var isApiCalled = apiCalled
    val lastCallTime = getSavedLastCallTime(context)
    val currentTime = System.currentTimeMillis()

    // Check if 1 second have passed
    if (currentTime - lastCallTime >= 10 * 60 * 1) {
        isApiCalled = false
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val apiKey = "1d17f66fa01ebd4dfa7c0aaa4d83e4e0"
                val latitude = 52.7721
                val longitude = 1.2058
                val apiUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=$latitude&lon=$longitude&appid=$apiKey"

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Update fetchWeatherData function
                    val weatherData = parseWeatherData(response.toString())
                    val weatherTime = parseWeatherTimes(response.toString(), context)
                    tempList.addAll(weatherData)
                    timeList.addAll(weatherTime)

                    // Save weather data to SharedPreferences
                    saveWeatherDataToStorage(context, weatherData, "weatherData")


                    // Update last call time
                    saveLastCallTime(context, currentTime)
                    isApiCalled = true
                } else {
                    Log.e("ScreenA", "HTTP Error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("ScreenA", "Error: ${e.message}")
            }
        }
    } else {
        Log.d("ScreenA", "Skipping API call; not enough time has passed since the last call.")
    }
    return isApiCalled
}

fun getSavedLastCallTime(context: Context): Long {
    val sharedPreferences = context.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
    return sharedPreferences.getLong("lastCallTime", 0)
}

fun saveLastCallTime(context: Context, lastCallTime: Long) {
    val sharedPreferences = context.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
    sharedPreferences.edit().putLong("lastCallTime", lastCallTime).apply()
}

// Grab and format the data
fun parseWeatherData(response: String): List<String> {
    val tempList = mutableListOf<String>()
    try {
        val jsonObject = JSONObject(response)
        val list = jsonObject.getJSONArray("list")

        var currentDay = ""
        var temperatures = ""

        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val dateTime = item.getString("dt_txt")
            val temperatureKelvin = item.getJSONObject("main").getDouble("temp")
            val temperatureCelsius = (temperatureKelvin - 273.15).toInt() // Convert to Celsius

            // Parse date from dateTime string
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(dateTime) ?: continue

            // Get day of the week
            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val dayOfWeek = dayOfWeekFormat.format(date)

            if (dayOfWeek != currentDay) {
                if (currentDay.isNotEmpty()) {
                    tempList.add("$currentDay, $temperatures")
                }
                currentDay = dayOfWeek
                temperatures = ""
            }
            if (temperatures.isNotEmpty()) temperatures += ", "
            temperatures += temperatureCelsius
        }

        if (temperatures.isNotEmpty()) {
            tempList.add("$currentDay, $temperatures")
        }

    } catch (e: Exception) {
        Log.e("ScreenA", "Error parsing JSON: ${e.message}")
    }
    return tempList
}

fun parseWeatherTimes(response: String, context: Context): List<String> {
    val timeList = mutableListOf<String>()
    try {
        val jsonObject = JSONObject(response)
        val list = jsonObject.getJSONArray("list")

        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val dateTime = item.getString("dt_txt")

            // Parse date from dateTime string
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(dateTime) ?: continue

            // Get time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.format(date)

            timeList.add(time)

        }
    } catch (e: Exception) {
        Log.e("ScreenA", "Error parsing JSON: ${e.message}")
    }
    saveWeatherDataToStorage(context, timeList,"weatherTime")
    return timeList
}


// Save weather data locally
private fun saveWeatherDataToStorage(context: Context, weatherData: List<String>, storageKey: String) {
    val sharedPreferences = context.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putStringSet(storageKey, weatherData.toSet())
    editor.apply()
}

// Retrieve weather data from local storage
private fun retrieveWeatherDataFromStorage(context: Context, storageKey: String): List<String> {
    val sharedPreferences = context.getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)
    val storedData = sharedPreferences.getStringSet(storageKey, setOf()) ?: setOf()
    return storedData.toList()
}


// Get coordinates of cities to compare with the users
suspend fun fetchCityCoordinatesA(apiKey: String): String {

    var name = ""

    withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.openweathermap.org/geo/1.0/reverse?lat=${lat.toString()}&lon=${lon.toString()}&limit=5&appid=$apiKey"
            val response = URL(apiUrl).readText(Charsets.UTF_8)
            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                name = jsonObject.getString("name")
                Log.d("Location", "Name: $name")
            }
        } catch (e: Exception) {
            Log.e("FetchCityCoordinates", "Error fetching city coordinates: ${e.message}")
            e.printStackTrace()
        }
    }

    return name
}


// Access to an outside app
fun goToWeatherA(context: Context?, lat:String, lon:String) {
    val url = "https://www.ventusky.com/?p=$lat;$lon;5&l=temperature-2m&w=off"
    goToUrl(context, url)
}

private fun goToUrl(context: Context?, url: String) {
    val uriUrl = Uri.parse(url)
    val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
    context?.startActivity(launchBrowser)
}




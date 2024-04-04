package com.example.testmk2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.round

var mainCity = ""
var selectedCityIndex = 0
var canSwipe = true
val api: String = "1d17f66fa01ebd4dfa7c0aaa4d83e4e0"
var result = false

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScreenB(navController: NavController, context: Context, newsContext: Context, locationViewModel: LocationViewModel) {
    // Define mutable state variable to hold the city
    var city by remember { mutableStateOf("Leicester") } // Assuming a default city
    var cities = arrayOf("London","Liverpool", "Leicester", "Manchester", "New York", "Paris", "Tokyo", "Berlin")

    // Get the users coordinates
    val lat = locationViewModel.lat
    val lon = locationViewModel.lon

    LaunchedEffect(Unit) {

        val fetchedCoordinates = fetchCityCoordinates(api)
        val givenCoordinates = Pair(lat, lon) // Provide your given coordinates here
        val closestCity = withContext(Dispatchers.Default) {
            findClosestCity(givenCoordinates, fetchedCoordinates)
        }

        // Find the index of the closest city
        val index = cities.indexOf(closestCity)
        mainCity = city
        if (index != -1) { // If closest city is found in the array
            // Create a new array to hold the rearranged elements
            val newArr = Array<String>(cities.size) { "" }

            // Move the city to the front
            newArr[0] = closestCity

            // Copy the remaining cities
            var newIndex = 1
            for (i in cities.indices) {
                if (i != index) {
                    newArr[newIndex] = cities[i]
                    newIndex++
                }
            }
            cities = newArr

            // Update the city state
            city = closestCity
        } else {
            Log.d("new","$closestCity not found.")
        }


    }

    // Define mutable state variable to hold the weather information
    var weatherDataInfo by remember { mutableStateOf(WeatherDataInfo()) }

    val coroutineScope = rememberCoroutineScope()

    // Update the UI with weather data
    fun updateUI(weatherData: WeatherDataInfo) {
        weatherDataInfo = weatherData
    }

    // Call the API every ten seconds using a coroutine
    LaunchedEffect(Unit) {
        while (true) {
            // Call the AsyncTask with the updateUI lambda function
            weatherClass(context, city) { weatherData -> updateUI(weatherData) }.execute()

            // Delay for a second
            delay(1000)
        }
    }

    // Page design
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .swipeToChangeCity(

                onSwipeLeft = {
                    if (canSwipe) {
                        selectedCityIndex++
                        if (selectedCityIndex >= cities.count()) {
                            selectedCityIndex = 0
                        }
                        city = cities[selectedCityIndex]
                        canSwipe = false
                    }
                },


                onSwipeRight = {
                    if (canSwipe) {
                        selectedCityIndex--
                        if (selectedCityIndex < 0) {
                            selectedCityIndex = cities.count() - 1
                        }
                        city = cities[selectedCityIndex]
                        canSwipe = false
                    }
                },

                coroutineBlock = {
                    delay(1000) // Adjust the delay time as needed
                    canSwipe = true
                }

            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF122259),
                        Color(0xFF9561a1)
                    ) // Replace with your gradient colors
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
                    navController.navigate(Routes.screenA)
                },
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 8.dp) // Adjust padding as needed
                    .weight(1f) // Occupy equal space as the other button
            ) {
                Text(text = "Current Weather")
            }

            // News button (right-aligned)
            Button(
                onClick = {
                    goToWeather(newsContext)
                },
                modifier = Modifier
                    .padding(start = 8.dp, top = 16.dp, end = 16.dp) // Adjust padding as needed
                    .weight(1f) // Occupy equal space as the other button
            ) {
                Text(text = "Weather")
            }
        }


        if(city == mainCity && result){
            Image(
                modifier = Modifier
                    .widthIn(min = 40.dp)
                    .heightIn(min = 40.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.home),
                contentDescription = "sunrise"
            )
        }
        // Location Text
        Text(
            text = weatherDataInfo.address,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )


        // Last updated time
        Text(
            text = weatherDataInfo.updateAtText,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Weather e.g. clear sky
        Text(
            text = weatherDataInfo.weatherDescription,
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Temperature
        Text(
            text = weatherDataInfo.temp,
            color = Color.White,
            fontSize = 75.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            // Min Temperature
            Text(
                text = weatherDataInfo.tempMin,
                color = Color.White,
                fontSize = 18.sp,
            )
            // Max Temperature
            Text(
                text = weatherDataInfo.tempMax,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 0.dp, start = 20.dp)
            )
        }

        // Top Row
        Row(
            modifier = Modifier
                .padding(top = 40.dp)
                .align(Alignment.CenterHorizontally)
        ) {

            Column {
                Box(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            bottom = 10.dp,
                            top = 10.dp,
                            end = 10.dp
                        ) // Padding outside the Box
                        .background(Color(0x5BFFFFFF)) // Translucent white color
                        .padding(10.dp)
                        .widthIn(min = 80.dp)
                        .heightIn(min = 80.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            text = "Sunrise",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                        Image(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .heightIn(min = 40.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = R.drawable.sunrise_icon),
                            contentDescription = "sunrise"
                        )
                        Text(
                            text = weatherDataInfo.sunrise,
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

            }
            Column {
                Box(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            bottom = 10.dp,
                            top = 10.dp,
                            end = 10.dp
                        ) // Padding outside the Box
                        .background(Color(0x5BFFFFFF)) // Translucent white color
                        .padding(10.dp)
                        .widthIn(min = 80.dp) // Maximum width
                        .heightIn(min = 80.dp) // Maximum height
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            text = "Sunset",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                        Image(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .heightIn(min = 40.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = R.drawable.sunset_icon),
                            contentDescription = "sunset"
                        )
                        Text(
                            text = weatherDataInfo.sunset,
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

            }
            Column {
                Box(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            bottom = 10.dp,
                            top = 10.dp,
                            end = 10.dp
                        ) // Padding outside the Box
                        .background(Color(0x5BFFFFFF)) // Translucent white color
                        .padding(10.dp)
                        .widthIn(min = 80.dp) // Maximum width
                        .heightIn(min = 80.dp) // Maximum height
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            text = "Wind",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                        Image(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .heightIn(min = 40.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = R.drawable.speed_icon),
                            contentDescription = "Wind Speed"
                        )
                        Text(
                            text = weatherDataInfo.windSpeed + "m/s",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

            }

        }

        // Second Row
        Row(
            modifier = Modifier
                .padding(top = 0.dp)
                .align(Alignment.CenterHorizontally)
        ) {

            Column {
                Box(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            bottom = 10.dp,
                            top = 10.dp,
                            end = 10.dp
                        ) // Padding outside the Box
                        .background(Color(0x5BFFFFFF)) // Translucent white color
                        .padding(10.dp)
                        .widthIn(min = 80.dp) // Maximum width
                        .heightIn(min = 80.dp) // Maximum height
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            text = "Humidity",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                        Image(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .heightIn(min = 40.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = R.drawable.humidity_icon),
                            contentDescription = "sunrise"
                        )
                        Text(
                            text = weatherDataInfo.humidity + "%",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

            }
            Column {
                Box(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            bottom = 10.dp,
                            top = 10.dp,
                            end = 10.dp
                        ) // Padding outside the Box
                        .background(Color(0x5BFFFFFF)) // Translucent white color
                        .padding(10.dp)
                        .widthIn(min = 80.dp) // Maximum width
                        .heightIn(min = 80.dp) // Maximum height
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            text = "Feels Like",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                        Image(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .heightIn(min = 40.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = R.drawable.temp_icon),
                            contentDescription = "Feels like temp"
                        )
                        Text(
                            text = weatherDataInfo.feelsLike,
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

            }
            Column {
                Box(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            bottom = 10.dp,
                            top = 10.dp,
                            end = 10.dp
                        ) // Padding outside the Box
                        .background(Color(0x5BFFFFFF)) // Translucent white color
                        .padding(10.dp)
                        .widthIn(min = 80.dp) // Maximum width
                        .heightIn(min = 80.dp) // Maximum height
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            text = "Pressure",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                        Image(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .heightIn(min = 40.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = R.drawable.pressure_icon),
                            contentDescription = "pressure"
                        )
                        Text(
                            text = weatherDataInfo.pressure + "hPa",
                            color = Color.Black,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }

            }
        }
    }

}

// Swipe gestures
@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun Modifier.swipeToChangeCity(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    coroutineBlock: suspend () -> Unit
): Modifier {
    return pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
            if (dragAmount < 0) {
                // Swiped left
                onSwipeLeft()
                GlobalScope.launch { coroutineBlock() }
            } else if (dragAmount > 0) {
                // Swiped right
                onSwipeRight()
                GlobalScope.launch { coroutineBlock() }
            }
        }
    }
}

// Access the api and retrieve all data
class weatherClass(private val context: Context, private val city: String, private val updateUI: (WeatherDataInfo) -> Unit) : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg params: String?): String? {
        return try {
            if (isInternetAvailable(context)) {
                // If internet is available, fetch data from the API
                URL("https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$api").readText(Charsets.UTF_8)
            } else {
                // If no internet, load data from the locally stored file
                loadDataFromFile(context, "weather_data.json")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        try {
            result?.let { jsonResult ->
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                val temp = round(main.getString("temp").toDouble()).toInt().toString() + "°C"
                val tempMin =
                    "Min Temp: " + round(main.getString("temp_min").toDouble()).toInt().toString() + "°C"
                val tempMax =
                    "Max Temp: " + round(main.getString("temp_max").toDouble()).toInt().toString() + "°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val feelsLike = round(main.getString("feels_like").toDouble()).toString() + "°C"
                val windSpeed = (round(wind.getString("speed").toDouble() * 10) / 10).toString()
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name") + ", " + sys.getString("country")

                val timezoneOffsetSeconds = jsonObj.getInt("timezone")
                val timezone = TimeZone.getTimeZone("UTC")
                val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

                val sunriseTimestamp = sys.getLong("sunrise") * 1000
                val sunsetTimestamp = sys.getLong("sunset") * 1000

                sdf.timeZone = timezone
                val sunrise = sdf.format(Date(sunriseTimestamp + timezoneOffsetSeconds * 1000))
                val sunset = sdf.format(Date(sunsetTimestamp + timezoneOffsetSeconds * 1000))

                val currentTimeMillis = System.currentTimeMillis()
                val timeInTimeZoneMillis = currentTimeMillis + timezoneOffsetSeconds * 1000
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timeInTimeZoneMillis

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)-1
                val minute = calendar.get(Calendar.MINUTE)
                val second = calendar.get(Calendar.SECOND)

                val formattedDay = if (day < 10) "0$day" else day.toString()
                val formattedMonth = if (month < 10) "0$month" else month.toString()
                val formattedMinute = if (minute < 10) "0$minute" else minute.toString()
                val formattedSecond = if (second < 10) "0$second" else second.toString()

                val updateAtText =
                    "$year:$formattedMonth:$formattedDay | $hour:$formattedMinute:$formattedSecond"

                val weatherDataInfo = WeatherDataInfo(
                    updateAtText = updateAtText,
                    temp = temp,
                    tempMin = tempMin,
                    tempMax = tempMax,
                    pressure = pressure,
                    humidity = humidity,
                    feelsLike = feelsLike,
                    sunrise = sunrise,
                    sunset = sunset,
                    windSpeed = windSpeed,
                    weatherDescription = weatherDescription,
                    address = address
                )

                updateUI(weatherDataInfo)

                val dataToSave = jsonObj.toString() // Convert JSON object to string
                val filename = "weather_data.json"
                saveDataToFile(context, dataToSave, filename)
            }
        } catch (e: Exception) {
            Log.d("datagotten", "data not retrieved")
            return
        }
    }


}

// Get coordinates of cities to compare with the users
suspend fun fetchCityCoordinates(apiKey: String): List<Pair<String, Pair<Double, Double>>> {
    val citiesWithCoordinates = mutableListOf<Pair<String, Pair<Double, Double>>>()

    withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.openweathermap.org/data/2.5/group?id=2643743,2644210,2644668,2643123,5128581,2988507,1850147,2950159&units=metric&appid=$apiKey"
            val response = URL(apiUrl).readText(Charsets.UTF_8)
            val jsonObj = JSONObject(response)

            val citiesArray = jsonObj.getJSONArray("list")
            Log.d("FetchCityCoordinates", "Number of cities in response: ${citiesArray.length()}")

            for (i in 0 until citiesArray.length()) {
                val cityObj = citiesArray.getJSONObject(i)
                val coord = cityObj.getJSONObject("coord")
                val cityId = cityObj.getInt("id")
                val address = cityObj.getString("name")
                val latitude = coord.getDouble("lat")
                val longitude = coord.getDouble("lon")
                citiesWithCoordinates.add(address to (latitude to longitude))
            }
        } catch (e: Exception) {
            Log.e("FetchCityCoordinates", "Error fetching city coordinates: ${e.message}")
            e.printStackTrace()
        }
    }

    Log.d("FetchCityCoordinates", "Number of cities with coordinates: ${citiesWithCoordinates.size}")
    return citiesWithCoordinates
}

// Function to find the closest city to the user
fun findClosestCity(coordinates: Pair<Double, Double>, citiesWithCoordinates: List<Pair<String, Pair<Double, Double>>>): String {
    var closestCity = ""
    var minDistance = Double.MAX_VALUE

    // Logging fetched coordinates
    Log.d("Coordinates", "Given Coordinates: ${coordinates.first}, ${coordinates.second}")
    Log.d("FindClosestCity", "Searching for closest city")
    Log.d("FindClosestCity", "Number of cities with coordinates: ${citiesWithCoordinates.size}") // New log statement
    for ((city, cityCoordinates) in citiesWithCoordinates) {
        // Logging city coordinates
        Log.d("Coordinates", "$city Coordinates: ${cityCoordinates.first}, ${cityCoordinates.second}")

        val distance = calculateDistance(coordinates.first, coordinates.second, cityCoordinates.first, cityCoordinates.second)

        // Logging calculated distance
        Log.d("Distance", "Distance to $city: $distance km")

        if (distance < minDistance) {
            minDistance = distance
            closestCity = city
        }
    }
    Log.d("ClosestCity", "Closest City: $closestCity")
    return closestCity
}

// Function to calculate distance between two sets of coordinates using Haversine formula
fun calculateDistance(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double {
    val R = 6371 // Radius of the earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

data class WeatherDataInfo(
    val updateAtText: String = "",
    val temp: String = "",
    val tempMin: String = "",
    val tempMax: String = "",
    val pressure: String = "",
    val humidity: String = "",
    val feelsLike: String = "",
    val sunrise: String = "",
    val sunset: String = "",
    val windSpeed: String = "",
    val weatherDescription: String = "",
    val address: String = ""
)

// Save data locally
fun saveDataToFile(context: Context, data: String, filename: String) {
    try {
        val outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
        outputStream.write(data.toByteArray())
        outputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Load the data from the local storage
fun loadDataFromFile(context: Context, filename: String): String? {
    return try {
        context.openFileInput(filename)?.bufferedReader()?.use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Check if theres access to the internet
fun isInternetAvailable(context: Context): Boolean {
    result = false
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager?.run {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                result = when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    else -> false
                }
            }
        }
    } else {
        connectivityManager?.run {
            connectivityManager.activeNetworkInfo?.run {
                if (type == ConnectivityManager.TYPE_WIFI) {
                    result = true
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    result = true
                }
            }
        }
    }
    return result
}

// Access to an outside app
fun goToWeather(context: Context?) {
    val url = "https://www.google.com/search?q=weather"
    goToUrl(context, url)
}

private fun goToUrl(context: Context?, url: String) {
    val uriUrl = Uri.parse(url)
    val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
    context?.startActivity(launchBrowser)
}
package com.example.testmk2

import android.annotation.SuppressLint
import android.content.ContentValues
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.round

var celsius = true
var canSwipe = true
val api: String = "1d17f66fa01ebd4dfa7c0aaa4d83e4e0"
var result = false

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScreenB(navController: NavController, context: Context, newsContext: Context, locationViewModel: LocationViewModel) {
    // Define mutable state variable to hold the city
    var city by remember { mutableStateOf("London") } // Assuming a default city
    var cities = arrayOf("London","Liverpool", "Leicester", "Manchester", "New York", "Paris", "Tokyo", "Berlin")

    // Get the users coordinates
    val lat = locationViewModel.lat
    val lon = locationViewModel.lon

    // Define mutable state variable to hold the weather information
    var weatherDataInfo by remember { mutableStateOf(WeatherDataInfo()) }

    val coroutineScope = rememberCoroutineScope()

    var airQuality by remember{ mutableStateOf("")}
    var airQualityDesc by remember{ mutableStateOf("")}

    // Update the UI with weather data
    fun updateUI(weatherData: WeatherDataInfo) {
        weatherDataInfo = weatherData
    }

    fun selectCity() {
        // Update the UI with weather data for the selected city
        Log.d("updating", city)
        weatherClass(context, city, celsius) { weatherData -> updateUI(weatherData) }.execute()
    }

    LaunchedEffect(Unit) {
        val fetchedCoordinates = fetchCityCoordinates(api)

        airQuality = fetchAirQuality(api, lat, lon)
        if(airQuality == "1"){airQualityDesc = "Good"}
        else if(airQuality == "2"){airQualityDesc = "Fair"}
        else if(airQuality == "3"){airQualityDesc = "Moderate"}
        else if(airQuality == "4"){airQualityDesc = "Poor"}
        else {airQualityDesc = "Very Poor"}

        val givenCoordinates = Pair(lat, lon) // Provide your given coordinates here
        val closestCity = withContext(Dispatchers.Default) {
            findClosestCity(givenCoordinates, fetchedCoordinates)
        }

        // Find the index of the closest city
        val index = cities.indexOf(closestCity)
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

        // Call createNotificationChannel
        NotificationHelper.createNotificationChannel(context)
        // Call scheduleNotifications
        NotificationHelper.scheduleNotifications(context, "Weather", weatherDataInfo.weatherDescription + " | Highs of " + weatherDataInfo.tempMax + " | Lows of " + weatherDataInfo.tempMin + " | Winds up to " + weatherDataInfo.windSpeed + "m/s")
    }




    // Call the API every ten seconds using a coroutine
    LaunchedEffect(Unit) {
        while (true) {
            // Call the AsyncTask with the updateUI lambda function
            weatherClass(context, city, celsius) { weatherData -> updateUI(weatherData) }.execute()

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
                    if (celsius) {
                        celsius = false
                    }
                },


                onSwipeRight = {
                    if (!celsius) {
                        celsius = true
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
                    goToWeather(newsContext, lat.toString(), lon.toString())
                },
                modifier = Modifier
                    .padding(start = 8.dp, top = 16.dp, end = 16.dp) // Adjust padding as needed
                    .weight(1f) // Occupy equal space as the other button
            ) {
                Text(text = "Weather")
            }
        }


        val locations = remember { mutableStateListOf<String>() }
        var searchText by remember { mutableStateOf("") }

        TextField(
            value = searchText,
            onValueChange = { newText ->
                searchText = newText
                locations.clear()
                // Call function to fetch data when the user types in the search bar
                fetchLocations(searchText, locations)
            },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textStyle = TextStyle(color = Color.White),
            placeholder = { Text(text = "Search...") },
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)// Adjust top padding to accommodate TextField
                .heightIn(max = 200.dp) // Set a minimum height of 200.dp
                .background(Color.Black) // Set background color
        ) {
            items(locations) { location ->
                Box(
                    modifier = Modifier
                        .clickable {
                            city = location.substring(4)
                            Log.d("CityChanger", city)
                            selectCity()
                            locations.clear()
                            searchText = ""

                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = location,
                        color = Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(end = 10.dp, start = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,

        ) {
            Box(
                //modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AQI $airQuality - $airQualityDesc",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            Box(
                contentAlignment = Alignment.Center

            ) {
                Image(
                    painter = if (celsius) painterResource(id = R.drawable.toggle_on) else painterResource(id = R.drawable.toggle_off),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
                Text(
                    text = if (celsius) "C" else "F",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = if(celsius) Modifier.align(Alignment.Center).offset(-8.dp, -0.5.dp) else Modifier.align(Alignment.Center).offset(8.dp, -0.5.dp)
                )
            }
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
                        .widthIn(min = 80.dp)
                        .heightIn(min = 80.dp)
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
                        .widthIn(min = 80.dp)
                        .heightIn(min = 80.dp)
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
                        .widthIn(min = 80.dp)
                        .heightIn(min = 80.dp)
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
                        .widthIn(min = 80.dp)
                        .heightIn(min = 80.dp)
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
                        .widthIn(min = 80.dp)
                        .heightIn(min = 80.dp)
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
class weatherClass(
    private val context: Context,
    private val city: String,
    private val celsius: Boolean,
    private val updateUI: (WeatherDataInfo) -> Unit
) : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg params: String?): String? {
        var loaded = false
        return try {
            if (isInternetAvailable(context)) {
                // If internet is available, fetch data from the API
                URL("https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$api").readText(
                    Charsets.UTF_8
                )
            } else {
                if (!loaded) {
                    // If no internet, load data from the locally stored file
                    val weatherDataList = loadDataFromContentProvider(context)
                    // Convert the weather data list to JSON format
                    updateUI(weatherDataList[0])
                    loaded = true

                }
                // Optionally, handle other cases if needed
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        try {
            context?.contentResolver?.delete(
                WeatherDataContract.CONTENT_URI,
                null,
                null
            ) // Clear existing data
            result?.let { jsonResult ->
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                var temp = ""
                var tempMin = ""
                var tempMax = ""
                var feelsLike = ""
                if (celsius) {
                    temp = round(main.getString("temp").toDouble()).toInt().toString() + "°C"
                    tempMin =
                        "Min Temp: " + round(main.getString("temp_min").toDouble()).toInt()
                            .toString() + "°C"
                    tempMax =
                        "Max Temp: " + round(main.getString("temp_max").toDouble()).toInt()
                            .toString() + "°C"
                    feelsLike = round(main.getString("feels_like").toDouble()).toString() + "°C"
                } else {
                    temp = round((main.getString("temp").toDouble() * 2) + 30).toInt()
                        .toString() + "°F"
                    tempMin =
                        "Min Temp: " + round(
                            (main.getString("temp_min").toDouble() * 2) + 30
                        ).toInt().toString() + "°F"
                    tempMax =
                        "Max Temp: " + round(
                            (main.getString("temp_max").toDouble() * 2) + 30
                        ).toInt().toString() + "°F"
                    feelsLike =
                        round((main.getString("feels_like").toDouble() * 2) + 30).toString() + "°F"
                }

                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")

                val windSpeed = (round(wind.getString("speed").toDouble() * 10) / 10).toString()
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name") + ", " + sys.getString("country")

                val timezoneOffsetSeconds = jsonObj.getInt("timezone")
                val timezone = TimeZone.getTimeZone("UTC")
                val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH) // Use 24-hour format

                val sunriseTimestamp = sys.getLong("sunrise") * 1000
                val sunsetTimestamp = sys.getLong("sunset") * 1000

                sdf.timeZone = timezone
                val sunrise =
                    sdf.format(Date(sunriseTimestamp + timezoneOffsetSeconds * 1000)) + " AM"
                val sunset =
                    sdf.format(Date(sunsetTimestamp + timezoneOffsetSeconds * 1000)) + " PM"


                val currentTimeMillis = System.currentTimeMillis()
                val timeInTimeZoneMillis = currentTimeMillis + timezoneOffsetSeconds * 1000
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timeInTimeZoneMillis

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY) - 1
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

                // Insert data into content provider
                val values = ContentValues().apply {
                    put(WeatherDataContract.Columns.UPDATE_AT, updateAtText)
                    put(WeatherDataContract.Columns.TEMP, temp)
                    put(WeatherDataContract.Columns.TEMP_MIN, tempMin)
                    put(WeatherDataContract.Columns.TEMP_MAX, tempMax)
                    put(WeatherDataContract.Columns.PRESSURE, pressure)
                    put(WeatherDataContract.Columns.HUMIDITY, humidity)
                    put(WeatherDataContract.Columns.FEELS_LIKE, feelsLike)
                    put(WeatherDataContract.Columns.SUNRISE, sunrise)
                    put(WeatherDataContract.Columns.SUNSET, sunset)
                    put(WeatherDataContract.Columns.WIND_SPEED, windSpeed)
                    put(WeatherDataContract.Columns.WEATHER_DESCRIPTION, weatherDescription)
                    put(WeatherDataContract.Columns.ADDRESS, address)
                }
                context?.contentResolver?.insert(WeatherDataContract.CONTENT_URI, values)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

    suspend fun fetchAirQuality(apiKey: String, lat: Double, lon: Double): String {
    var airQuality = ""
    withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.openweathermap.org/data/2.5/air_pollution?lat=$lat&lon=$lon&appid=$apiKey"
            val response = URL(apiUrl).readText(Charsets.UTF_8)
            val jsonObj = JSONObject(response)

            // Get the first object in the "list" array
            val list = jsonObj.getJSONArray("list")
            if (list.length() > 0) {
                val firstItem = list.getJSONObject(0)
                val main = firstItem.getJSONObject("main")
                // Get the AQI as a string
                airQuality = main.getInt("aqi").toString()
            }
        } catch (e: Exception) {
            Log.e("fetchAirQuality", "Error fetching air quality data: ${e.message}")
            e.printStackTrace()
        }
    }
    return airQuality
}

fun fetchLocations(query: String, locations: MutableList<String>) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.openweathermap.org/geo/1.0/direct?q=$query&limit=5&appid=$api"
            val response = URL(apiUrl).readText(Charsets.UTF_8)
            Log.d("Locations", "Response: $response")
            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val name = jsonObject.getString("name")
                val country = jsonObject.getString("country")
                val state = jsonObject.optString("state", "")
                println("Name: $name, Country: $country, State: $state")
                val info = "$country, $name"
                locations.add(info)
            }
            for(i in 0 until locations.count()){
                Log.d("Locations1: ", locations[i])
            }
        } catch (e: Exception) {
            Log.e("fetchLocations", "Error fetching locations data: ${e.message}")
            e.printStackTrace()
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
fun saveDataToFile(context: Context, weatherData: WeatherDataInfo) {
    val contentResolver = context.contentResolver

    val values = ContentValues().apply {
        put("updateAtText", weatherData.updateAtText)
        put("temp", weatherData.temp)
        put("tempMin", weatherData.tempMin)
        put("tempMax", weatherData.tempMax)
        put("pressure", weatherData.pressure)
        put("humidity", weatherData.humidity)
        put("feelsLike", weatherData.feelsLike)
        put("sunrise", weatherData.sunrise)
        put("sunset", weatherData.sunset)
        put("windSpeed", weatherData.windSpeed)
        put("weatherDescription", weatherData.weatherDescription)
        put("address", weatherData.address)
    }

    val uri = Uri.parse("content://com.example.weatherprovider/weather")
    contentResolver.insert(uri, values)
}

// Load the data from the local storage
private fun loadDataFromContentProvider(context: Context): List<WeatherDataInfo> {
    val contentResolver = context.contentResolver
    val projection = arrayOf(
        WeatherDataContract.Columns.UPDATE_AT,
        WeatherDataContract.Columns.TEMP,
        WeatherDataContract.Columns.TEMP_MIN,
        WeatherDataContract.Columns.TEMP_MAX,
        WeatherDataContract.Columns.PRESSURE,
        WeatherDataContract.Columns.HUMIDITY,
        WeatherDataContract.Columns.FEELS_LIKE,
        WeatherDataContract.Columns.SUNRISE,
        WeatherDataContract.Columns.SUNSET,
        WeatherDataContract.Columns.WIND_SPEED,
        WeatherDataContract.Columns.WEATHER_DESCRIPTION,
        WeatherDataContract.Columns.ADDRESS
    )
    val cursor = contentResolver.query(
        WeatherDataContract.CONTENT_URI,
        projection,
        null,
        null,
        null
    )

    val weatherDataList = mutableListOf<WeatherDataInfo>()
    cursor?.use {
        while (it.moveToNext()) {
            val updateAt = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.UPDATE_AT))
            val temp = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.TEMP))
            val tempMin = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.TEMP_MIN))
            val tempMax = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.TEMP_MAX))
            val pressure = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.PRESSURE))
            val humidity = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.HUMIDITY))
            val feelsLike = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.FEELS_LIKE))
            val sunrise = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.SUNRISE))
            val sunset = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.SUNSET))
            val windSpeed = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.WIND_SPEED))
            val weatherDescription = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.WEATHER_DESCRIPTION))
            val address = it.getString(it.getColumnIndexOrThrow(WeatherDataContract.Columns.ADDRESS))

            val weatherData = WeatherDataInfo(
                updateAtText = updateAt,
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
            weatherDataList.add(weatherData)

            // Add a log statement to output each row of data
            Log.d("WeatherData", "Loaded data from content provider: $weatherData")
        }
    }

    return weatherDataList
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
fun goToWeather(context: Context?, lat:String, lon:String) {
    val url = "https://www.ventusky.com/?p=$lat;$lon;5&l=temperature-2m&w=off"
    goToUrl(context, url)
}

private fun goToUrl(context: Context?, url: String) {
    val uriUrl = Uri.parse(url)
    val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
    context?.startActivity(launchBrowser)
}

object WeatherDataContract {
    const val AUTHORITY = "com.example.weather.provider"
    const val PATH_WEATHER = "weather"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_WEATHER")

    object Columns {
        const val ID = "_id"
        const val UPDATE_AT = "update_at"
        const val TEMP = "temp"
        const val TEMP_MIN = "temp_min"
        const val TEMP_MAX = "temp_max"
        const val PRESSURE = "pressure"
        const val HUMIDITY = "humidity"
        const val FEELS_LIKE = "feels_like"
        const val SUNRISE = "sunrise"
        const val SUNSET = "sunset"
        const val WIND_SPEED = "wind_speed"
        const val WEATHER_DESCRIPTION = "weather_description"
        const val ADDRESS = "address"
    }
}

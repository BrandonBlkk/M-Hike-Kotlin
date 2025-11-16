package com.example.hikermanagementapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hikermanagementapplication.databinding.ActivityWeatherForecastBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class WeatherForecastActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherForecastBinding

    // Location Services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Adapters
    private lateinit var hourlyAdapter: HourlyWeatherAdapter
    private lateinit var dailyAdapter: DailyWeatherAdapter
    private lateinit var tipsAdapter: WeatherTipsAdapter

    // Data
    private val hourlyWeatherList = ArrayList<HourlyWeather>()
    private val dailyWeatherList = ArrayList<DailyWeather>()
    private val additionalInfoList = ArrayList<AdditionalInfo>()
    private val weatherTipsList = ArrayList<WeatherTip>()

    // Location permission
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val REQUEST_CHECK_SETTINGS = 1002

    // API Configuration
    private val API_KEY = "6d5afd002c34b8a5ffce535140978ca3"
    private val BASE_URL = "https://api.openweathermap.org/data/2.5/forecast"
    private val GEO_URL = "https://api.openweathermap.org/geo/1.0/direct"

    // Search
    private var searchDialog: AlertDialog? = null
    private var searchResults = ArrayList<CitySearchResult>()
    private lateinit var searchAdapter: CitySearchAdapter

    companion object {
        private const val TAG = "WeatherForecastActivity"
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupLocationServices()
        setupClickListeners()
        setupBottomNavigation()
        setupSearch()
        updateTimeAndDate()

        // Check location permission
        if (checkLocationPermission()) {
            requestLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun setupRecyclerViews() {
        // Hourly forecast
        hourlyAdapter = HourlyWeatherAdapter(hourlyWeatherList)
        binding.hourlyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.hourlyRecyclerView.adapter = hourlyAdapter

        // Daily forecast
        dailyAdapter = DailyWeatherAdapter(dailyWeatherList)
        binding.dailyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dailyRecyclerView.adapter = dailyAdapter

        // Weather tips
        tipsAdapter = WeatherTipsAdapter(weatherTipsList)
        binding.tipsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tipsRecyclerView.adapter = tipsAdapter
    }

    private fun setupSearch() {
        searchAdapter = CitySearchAdapter(searchResults) { city ->
            searchDialog?.dismiss()
            getWeatherByCityName(city.name, city.country)
        }
    }

    // Bottom navigation
    private fun setupBottomNavigation() {
        // Set the Weather tab as selected
        binding.bottomNavigation.selectedItemId = R.id.navWeather

        // Set up navigation item selection listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    // Navigate to Home activity
                    finish()
                    true
                }
                R.id.navMyHike -> {
                    // Navigate to HikeListActivity
                    val intent = android.content.Intent(this, HikeListActivity::class.java)
                    startActivity(intent)
                    finish() // Close current activity
                    true
                }
                R.id.navAddHike -> {
                    // Navigate to AddHikeActivity
                    val intent = android.content.Intent(this, AddHikeActivity::class.java)
                    startActivity(intent)
                    finish() // Close current activity
                    true
                }
                R.id.navMap -> {
                    // Navigate to AddObservationActivity
                    val intent = android.content.Intent(this, AddObservationActivity::class.java)
                    startActivity(intent)
                    finish() // Close current activity
                    true
                }
                R.id.navWeather -> {
                    true
                }
                else -> false
            }
        }

        // Remove any default selection listeners that might interfere
        binding.bottomNavigation.setOnItemSelectedListener(null)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    val intent = android.content.Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navMyHike -> {
                    val intent = android.content.Intent(this, HikeListActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navAddHike -> {
                    val intent = android.content.Intent(this, AddHikeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navMap -> {
                    val intent = android.content.Intent(this, AddObservationActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navWeather -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    getWeatherData(location.latitude, location.longitude)
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.refreshLayout.setOnRefreshListener {
            refreshWeatherData()
        }

        binding.retryButton.setOnClickListener {
            refreshWeatherData()
        }

        binding.searchButton.setOnClickListener {
            showSearchDialog()
        }
    }

    private fun showSearchDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_city_search, null)
        val searchEditText = dialogView.findViewById<EditText>(R.id.searchEditText)
        val searchResultsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        val useCurrentLocationButton = dialogView.findViewById<Button>(R.id.useCurrentLocationButton)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.searchProgressBar)

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = searchAdapter

        // Setup search text watcher with debounce
        val handler = Handler(Looper.getMainLooper())
        var searchRunnable: Runnable? = null

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = s.toString().trim()
                    if (query.length >= 2) {
                        searchCities(query, progressBar)
                    } else {
                        searchResults.clear()
                        searchAdapter.notifyDataSetChanged()
                    }
                }
                handler.postDelayed(searchRunnable!!, 500) // 500ms debounce
            }
        })

        useCurrentLocationButton.setOnClickListener {
            searchDialog?.dismiss()
            if (checkLocationPermission()) {
                requestLocationUpdates()
            } else {
                requestLocationPermission()
            }
        }

        searchDialog = AlertDialog.Builder(this)
            .setTitle("Search City")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun searchCities(query: String, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE

        thread {
            try {
                val urlString = "$GEO_URL?q=$query&limit=5&appid=$API_KEY"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    val citiesArray = JSONObject("{\"cities\": $response}").getJSONArray("cities")
                    searchResults.clear()

                    for (i in 0 until citiesArray.length()) {
                        val city = citiesArray.getJSONObject(i)
                        val name = city.getString("name")
                        val country = city.getString("country")
                        val state = if (city.has("state")) city.getString("state") else ""
                        val lat = city.getDouble("lat")
                        val lon = city.getDouble("lon")

                        searchResults.add(CitySearchResult(name, state, country, lat, lon))
                    }

                    runOnUiThread {
                        searchAdapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                    }
                } else {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error searching cities: ${e.message}")
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getWeatherByCityName(cityName: String, country: String) {
        showLoadingState()

        thread {
            try {
                val urlString = "$BASE_URL?q=$cityName&appid=$API_KEY&units=metric"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    val weatherData = JSONObject(response)
                    runOnUiThread {
                        updateWeatherUI(weatherData)
                        showContentState()
                        Toast.makeText(this, "Weather for $cityName, $country", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        showErrorState("City not found: $cityName")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather by city: ${e.message}")
                runOnUiThread {
                    showErrorState("Error loading weather for $cityName")
                }
            }
        }
    }

    private fun updateTimeAndDate() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE MMM dd, HH:mm", Locale.getDefault())

        val currentTime = Date()
        binding.timeText.text = timeFormat.format(currentTime)
        binding.dateText.text = dateFormat.format(currentTime)

        // Update time every minute
        Handler(Looper.getMainLooper()).postDelayed({
            updateTimeAndDate()
        }, 60000)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        showLoadingState()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@WeatherForecastActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: Exception) {
                    // Ignore the error
                    getLastKnownLocation()
                }
            } else {
                getLastKnownLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        getWeatherData(location.latitude, location.longitude)
                    } else {
                        showErrorState("Unable to get current location")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting last location: ${exception.message}")
                    showErrorState("Location error: ${exception.message}")
                }
        } else {
            showErrorState("Location permission denied")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates()
                } else {
                    showErrorState("Location permission is required to show weather data")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                when (resultCode) {
                    RESULT_OK -> {
                        // Location settings were successfully changed, try again
                        requestLocationUpdates()
                    }
                    RESULT_CANCELED -> {
                        // User declined to change location settings, try with last known location
                        getLastKnownLocation()
                    }
                }
            }
        }
    }

    private fun getWeatherData(latitude: Double, longitude: Double) {
        showLoadingState()

        thread {
            try {
                val urlString = "$BASE_URL?lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    val weatherData = JSONObject(response)
                    runOnUiThread {
                        updateWeatherUI(weatherData)
                        showContentState()
                    }
                } else {
                    runOnUiThread {
                        showErrorState("API Error: $responseCode - ${connection.responseMessage}")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather data: ${e.message}")
                runOnUiThread {
                    showErrorState("Network error: ${e.message}")
                }
            }
        }
    }

    private fun refreshWeatherData() {
        binding.refreshLayout.isRefreshing = true
        if (checkLocationPermission()) {
            getLastKnownLocation()
        } else {
            showErrorState("Location permission required")
            binding.refreshLayout.isRefreshing = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWeatherUI(weatherData: JSONObject) {
        try {
            // Extract current weather from the first forecast item
            val currentWeather = weatherData.getJSONArray("list").getJSONObject(0)
            val main = currentWeather.getJSONObject("main")
            val weather = currentWeather.getJSONArray("weather").getJSONObject(0)
            val wind = currentWeather.getJSONObject("wind")
            val city = weatherData.getJSONObject("city")

            // Update current weather
            val currentTemp = main.getDouble("temp").toInt()
            val feelsLike = main.getDouble("feels_like").toInt()
            val humidity = main.getInt("humidity")
            val pressure = main.getInt("pressure")
            val windSpeed = wind.getDouble("speed").toInt()
            val description = weather.getString("description").uppercase()
            val cityName = city.getString("name")
            val country = city.getString("country")

            binding.currentTempText.text = "${currentTemp}°"
            binding.feelsLikeText.text = "Feels like ${feelsLike}°"
            binding.weatherDescriptionText.text = description
            binding.humidityText.text = "$humidity%"
            binding.pressureText.text = pressure.toString()
            binding.windText.text = windSpeed.toString()
            binding.locationText.text = "$cityName, $country"

            // Update hourly data
            updateHourlyData(weatherData)

            // Update daily data
            updateDailyData(weatherData)

            // Update weather tips
            updateWeatherTips(currentTemp, description, windSpeed, humidity)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weather data: ${e.message}")
            showErrorState("Error parsing weather data")
        }

        binding.refreshLayout.isRefreshing = false
    }

    private fun updateHourlyData(weatherData: JSONObject) {
        hourlyWeatherList.clear()
        val forecastList = weatherData.getJSONArray("list")
        val timeFormat = SimpleDateFormat("HH", Locale.getDefault())

        for (i in 0 until minOf(8, forecastList.length())) {
            val forecast = forecastList.getJSONObject(i)
            val main = forecast.getJSONObject("main")
            val weather = forecast.getJSONArray("weather").getJSONObject(0)

            val date = Date(forecast.getLong("dt") * 1000)
            val time = if (i == 0) "Now" else timeFormat.format(date)
            val temp = "${main.getDouble("temp").toInt()}°"
            val iconRes = getWeatherIcon(weather.getString("main"))

            hourlyWeatherList.add(HourlyWeather(time, temp, iconRes))
        }
        hourlyAdapter.notifyDataSetChanged()
    }

    private fun updateDailyData(weatherData: JSONObject) {
        dailyWeatherList.clear()
        val forecastList = weatherData.getJSONArray("list")
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Group forecasts by day
        val dailyTemps = mutableMapOf<String, Pair<Double, Double>>() // Date to (minTemp, maxTemp)
        val dailyIcons = mutableMapOf<String, String>() // Date to weather condition

        for (i in 0 until forecastList.length()) {
            val forecast = forecastList.getJSONObject(i)
            val main = forecast.getJSONObject("main")
            val weather = forecast.getJSONArray("weather").getJSONObject(0)

            val date = Date(forecast.getLong("dt") * 1000)
            val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val temp = main.getDouble("temp")

            if (!dailyTemps.containsKey(dayKey)) {
                dailyTemps[dayKey] = Pair(temp, temp)
                dailyIcons[dayKey] = weather.getString("main")
            } else {
                val (min, max) = dailyTemps[dayKey]!!
                dailyTemps[dayKey] = Pair(minOf(min, temp), maxOf(max, temp))
            }
        }

        // Create daily forecast items
        val sortedDays = dailyTemps.keys.sorted()
        for (i in sortedDays.indices) {
            val dayKey = sortedDays[i]
            val (minTemp, maxTemp) = dailyTemps[dayKey]!!
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayKey)
            val dayName = if (i == 0) "Today" else dateFormat.format(date)
            val iconRes = getWeatherIcon(dailyIcons[dayKey]!!)

            dailyWeatherList.add(DailyWeather(dayName, "${maxTemp.toInt()}°", "${minTemp.toInt()}°", iconRes))
        }
        dailyAdapter.notifyDataSetChanged()
    }

    private fun updateWeatherTips(currentTemp: Int, description: String, windSpeed: Int, humidity: Int) {
        weatherTipsList.clear()

        // Temperature-based tips
        when {
            currentTemp > 30 -> weatherTipsList.add(WeatherTip("Very hot! Stay hydrated and avoid strenuous activities during peak hours", R.drawable.information_line))
            currentTemp > 20 -> weatherTipsList.add(WeatherTip("Perfect weather for hiking! Wear light clothing and stay hydrated", R.drawable.information_line))
            currentTemp > 10 -> weatherTipsList.add(WeatherTip("Pleasant weather. A light jacket might be useful", R.drawable.information_line))
            else -> weatherTipsList.add(WeatherTip("Cold weather. Dress in layers and wear warm clothing", R.drawable.information_line))
        }

        // Weather condition tips
        when {
            description.contains("RAIN", true) -> weatherTipsList.add(WeatherTip("Rain expected. Bring waterproof gear and wear appropriate footwear", R.drawable.information_line))
            description.contains("CLOUD", true) -> weatherTipsList.add(WeatherTip("Cloudy conditions. Good for hiking without strong sun exposure", R.drawable.information_line))
            description.contains("CLEAR", true) -> weatherTipsList.add(WeatherTip("Clear skies. Don't forget sunscreen and sunglasses", R.drawable.information_line))
            description.contains("WIND", true) -> weatherTipsList.add(WeatherTip("Windy conditions. Secure loose items and be cautious on exposed trails", R.drawable.information_line))
        }

        // Wind speed tips
        if (windSpeed > 20) {
            weatherTipsList.add(WeatherTip("Strong winds expected. Be careful on exposed ridges and mountain tops", R.drawable.information_line))
        }

        // Humidity tips
        when {
            humidity > 80 -> weatherTipsList.add(WeatherTip("High humidity. Take frequent breaks and drink plenty of water", R.drawable.information_line))
            humidity < 30 -> weatherTipsList.add(WeatherTip("Low humidity. Stay hydrated and use lip balm", R.drawable.information_line))
        }

        tipsAdapter.notifyDataSetChanged()
    }

    private fun getWeatherIcon(weatherMain: String): Int {
        return when (weatherMain.uppercase()) {
            "CLEAR" -> R.drawable.sun_fill
            "CLOUDS" -> R.drawable.cloudy_fill
            "RAIN", "DRIZZLE" -> R.drawable.rain_fill
            "THUNDERSTORM" -> R.drawable.thunderstorms_fill
            "SNOW" -> R.drawable.snowy_fill
            "MIST", "FOG", "HAZE" -> R.drawable.mist_fill
            else -> R.drawable.sun_cloudy
        }
    }

    private fun showLoadingState() {
        binding.loadingContainer.visibility = android.view.View.VISIBLE
        binding.contentContainer.visibility = android.view.View.GONE
        binding.errorContainer.visibility = android.view.View.GONE
    }

    private fun showContentState() {
        binding.loadingContainer.visibility = android.view.View.GONE
        binding.contentContainer.visibility = android.view.View.VISIBLE
        binding.errorContainer.visibility = android.view.View.GONE
    }

    private fun showErrorState(errorMessage: String) {
        binding.loadingContainer.visibility = android.view.View.GONE
        binding.contentContainer.visibility = android.view.View.GONE
        binding.errorContainer.visibility = android.view.View.VISIBLE
        binding.errorText.text = errorMessage
        binding.refreshLayout.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        // Ensure Weather tab is selected when activity resumes
        binding.bottomNavigation.selectedItemId = R.id.navWeather
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        searchDialog?.dismiss()
    }
}

// Data classes
data class HourlyWeather(
    val time: String,
    val temperature: String,
    val iconRes: Int
)

data class DailyWeather(
    val day: String,
    val maxTemp: String,
    val minTemp: String,
    val iconRes: Int
)

data class AdditionalInfo(
    val value: String,
    val label: String,
    val description: String,
    val iconRes: Int
)

data class WeatherTip(
    val tip: String,
    val iconRes: Int
)

data class CitySearchResult(
    val name: String,
    val state: String,
    val country: String,
    val lat: Double,
    val lon: Double
)

// Search Adapter
class CitySearchAdapter(
    private val cities: List<CitySearchResult>,
    private val onCityClick: (CitySearchResult) -> Unit
) : RecyclerView.Adapter<CitySearchAdapter.CityViewHolder>() {

    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cityName: TextView = itemView.findViewById(R.id.cityName)
        val cityDetails: TextView = itemView.findViewById(R.id.cityDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_search, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.cityName.text = city.name
        holder.cityDetails.text = if (city.state.isNotEmpty()) {
            "${city.state}, ${city.country}"
        } else {
            city.country
        }

        holder.itemView.setOnClickListener {
            onCityClick(city)
        }
    }

    override fun getItemCount(): Int = cities.size
}
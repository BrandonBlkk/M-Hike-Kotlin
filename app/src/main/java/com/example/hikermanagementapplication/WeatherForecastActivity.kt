package com.example.hikermanagementapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.hikermanagementapplication.databinding.ActivityWeatherForecastBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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

    companion object {
        private const val TAG = "WeatherForecastActivity"
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupRecyclerViews()
        setupLocationServices()
        setupClickListeners()
        setupBottomNavigation()
        updateTimeAndDate()

        // Check location permission
        if (checkLocationPermission()) {
            requestLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun initializeViews() {
        // All views are now accessible through binding
        // No need to initialize individual views with findViewById
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

    // Add this new method to setup bottom navigation
    private fun setupBottomNavigation() {
        // Set the Weather tab as selected
        binding.bottomNavigation.selectedItemId = R.id.navWeather

        // Set up navigation item selection listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    // Navigate to Home activity
                    finish() // Close current activity and go back to home
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
                    // Already on Weather screen, just mark as selected
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
                    val intent = android.content.Intent(this, MainActivity::class.java) // Replace with your home activity
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
                    // Already on weather screen, do nothing but keep it selected
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
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
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

        // Simulate API call - Replace with actual weather API implementation
        Handler(Looper.getMainLooper()).postDelayed({
            // Mock data for demonstration
            updateWeatherUI(mockWeatherData())
            showContentState()
        }, 2000)
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

    private fun updateWeatherUI(weatherData: JSONObject) {
        // Update current weather
        binding.currentTempText.text = "24°"
        binding.feelsLikeText.text = "Feels like 26°"
        binding.weatherDescriptionText.text = "CLEAR SKY"
        binding.humidityText.text = "65%"
        binding.pressureText.text = "1013"
        binding.windText.text = "15"

        // Update mock location
        binding.locationText.text = "New York, US"

        // Update hourly data
        hourlyWeatherList.clear()
        hourlyWeatherList.addAll(getMockHourlyData())
        hourlyAdapter.notifyDataSetChanged()

        // Update daily data
        dailyWeatherList.clear()
        dailyWeatherList.addAll(getMockDailyData())
        dailyAdapter.notifyDataSetChanged()

        // Update weather tips
        weatherTipsList.clear()
        weatherTipsList.addAll(getMockWeatherTips())
        tipsAdapter.notifyDataSetChanged()

        binding.refreshLayout.isRefreshing = false
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

    // Mock data methods - Replace with actual API data
    private fun mockWeatherData(): JSONObject {
        return JSONObject().apply {
            put("temperature", 24)
            put("feels_like", 26)
            put("description", "Clear sky")
            put("humidity", 65)
            put("pressure", 1013)
            put("wind_speed", 15)
        }
    }

    private fun getMockHourlyData(): List<HourlyWeather> {
        return listOf(
            HourlyWeather("Now", "24°", R.drawable.sun_fill),
            HourlyWeather("15", "23°", R.drawable.sun_fill),
            HourlyWeather("16", "22°", R.drawable.sun_cloudy),
            HourlyWeather("17", "21°", R.drawable.sun_cloudy),
            HourlyWeather("18", "20°", R.drawable.cloudy_fill),
            HourlyWeather("19", "19°", R.drawable.cloudy_fill),
            HourlyWeather("20", "18°", R.drawable.cloudy_fill)
        )
    }

    private fun getMockDailyData(): List<DailyWeather> {
        return listOf(
            DailyWeather("Today", "24°", "18°", R.drawable.sun_fill),
            DailyWeather("Thu", "23°", "17°", R.drawable.sun_cloudy),
            DailyWeather("Fri", "22°", "16°", R.drawable.cloudy_fill),
            DailyWeather("Sat", "21°", "15°", R.drawable.rain_fill),
            DailyWeather("Sun", "20°", "14°", R.drawable.rain_fill),
            DailyWeather("Mon", "22°", "15°", R.drawable.sun_cloudy)
        )
    }

    private fun getMockAdditionalInfo(): List<AdditionalInfo> {
        return listOf(
            AdditionalInfo("8,234", "Steps", "Active day!", R.drawable.walk_line),
            AdditionalInfo("85%", "Humidity", "Comfortable", R.drawable.humidity_line),
            AdditionalInfo("15", "Wind", "km/h", R.drawable.windy_line),
            AdditionalInfo("UV", "Index", "Low", R.drawable.sun_line)
        )
    }

    private fun getMockWeatherTips(): List<WeatherTip> {
        return listOf(
            WeatherTip("Perfect weather for outdoor activities!", R.drawable.information_line),
            WeatherTip("Stay hydrated throughout the day", R.drawable.information_line),
            WeatherTip("Light jacket recommended for evening", R.drawable.information_line)
        )
    }

    override fun onResume() {
        super.onResume()
        // Ensure Weather tab is selected when activity resumes
        binding.bottomNavigation.selectedItemId = R.id.navWeather
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

// Data classes (keep your existing data classes)
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
package com.example.hikermanagementapplication

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.hikermanagementapplication.databinding.ActivityEditHikeBinding
import java.util.Calendar

class EditHikeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditHikeBinding
    private lateinit var dbHelper: HikeDbHelper

    private var isCompleted = 0
    private var completedDate: String? = null
    private var currentHikeId: Long = -1
    private lateinit var currentHike: Hike

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHikeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Get hike ID
        currentHikeId = intent.getLongExtra("hikeId", -1L)
        if (currentHikeId == -1L) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load hike details
        val hike = dbHelper.getHikeById(currentHikeId)
        if (hike == null) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentHike = hike
        setupForm(hike)
        setupSpinners()
        setupListeners()
    }

    private fun setupForm(hike: Hike) {
        // Set hike information
        binding.etHikeName.setText(hike.name)
        binding.etLocation.setText(hike.location)
        binding.etLength.setText(hike.length.toString())
        binding.etWeather.setText(hike.weather ?: "")
        binding.etDescription.setText(hike.description ?: "")
        binding.etNotes.setText(hike.notes ?: "")

        // Set dates
        binding.tvSelectedDate.text = hike.date
        binding.tvSelectedDate.setTextColor(Color.BLACK)

        // Set completion status
        isCompleted = hike.isCompleted
        completedDate = hike.completedDate
        binding.switchHikeCompleted.isChecked = hike.isCompleted == 1

        if (hike.isCompleted == 1 && !hike.completedDate.isNullOrEmpty()) {
            binding.tvCompletedDate.text = hike.completedDate
            binding.tvCompletedDate.setTextColor(Color.BLACK)
        }

        toggleCompletedStatus(hike.isCompleted == 1)
    }

    private fun setupSpinners() {
        // Parking Spinner
        val parkingOptions = listOf("Select parking option", "Yes", "No")
        val parkingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parkingOptions)
        parkingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spParking.adapter = parkingAdapter

        // Set selected parking
        val parkingPosition = when (currentHike.parking) {
            "Yes" -> 1
            "No" -> 2
            else -> 0
        }
        binding.spParking.setSelection(parkingPosition)

        // Difficulty Spinner
        val difficultyOptions = listOf("Select difficulty level", "Easy", "Moderate", "Hard")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficultyOptions)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDifficulty.adapter = difficultyAdapter

        // Set selected difficulty
        val difficultyPosition = when (currentHike.difficulty) {
            "Easy" -> 1
            "Moderate" -> 2
            "Hard" -> 3
            else -> 0
        }
        binding.spDifficulty.setSelection(difficultyPosition)

        // Route Type Spinner
        val routeTypeOptions = listOf("Select route type", "Loop", "Out & Back", "Point to Point", "Lollipop")
        val routeTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeTypeOptions)
        routeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRouteType.adapter = routeTypeAdapter

        // Set selected route type
        val routeTypePosition = when (currentHike.routeType) {
            "Loop" -> 1
            "Out & Back" -> 2
            "Point to Point" -> 3
            "Lollipop" -> 4
            else -> 0
        }
        binding.spRouteType.setSelection(routeTypePosition)
    }

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Date Picker for planned hike
        binding.layoutPickDate.setOnClickListener {
            showDatePicker(binding.tvSelectedDate, "Select hike date")
        }

        // Completed Date Picker
        binding.layoutPickCompletedDate.setOnClickListener {
            showDatePicker(binding.tvCompletedDate, "Select completed date")
        }

        // Hike Completed Toggle
        binding.switchHikeCompleted.setOnCheckedChangeListener { _, isChecked ->
            toggleCompletedStatus(isChecked)
        }

        // Update Button
        binding.btnUpdateHike.setOnClickListener {
            validateAndUpdateHike()
        }
    }

    private fun validateAndUpdateHike(): Boolean {
        val hikeName = binding.etHikeName.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val parking = binding.spParking.selectedItem.toString()
        val difficulty = binding.spDifficulty.selectedItem.toString()
        val routeType = binding.spRouteType.selectedItem.toString()
        val lengthStr = binding.etLength.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val weather = binding.etWeather.text.toString().trim()
        val date = binding.tvSelectedDate.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        var hasError = false

        // Clear previous errors
        binding.tvHikeNameError.visibility = android.view.View.GONE
        binding.tvLocationError.visibility = android.view.View.GONE
        binding.tvDateError.visibility = android.view.View.GONE
        binding.tvParkingError.visibility = android.view.View.GONE
        binding.tvLengthError.visibility = android.view.View.GONE
        binding.tvDifficultyError.visibility = android.view.View.GONE
        binding.tvCompletedDateError.visibility = android.view.View.GONE

        // Validations
        if (hikeName.isEmpty()) {
            binding.tvHikeNameError.visibility = android.view.View.VISIBLE
            hasError = true
        }
        if (location.isEmpty()) {
            binding.tvLocationError.visibility = android.view.View.VISIBLE
            hasError = true
        }
        if (date == "Select date" || date.isEmpty()) {
            binding.tvDateError.visibility = android.view.View.VISIBLE
            hasError = true
        }
        if (parking == "Select parking option") {
            binding.tvParkingError.visibility = android.view.View.VISIBLE
            hasError = true
        }
        if (lengthStr.isEmpty()) {
            binding.tvLengthError.visibility = android.view.View.VISIBLE
            binding.tvLengthError.text = "Length is required"
            hasError = true
        } else if (lengthStr.toDoubleOrNull() == null || lengthStr.toDouble() <= 0) {
            binding.tvLengthError.visibility = android.view.View.VISIBLE
            binding.tvLengthError.text = "Length must be a valid number"
            hasError = true
        }
        if (difficulty == "Select difficulty level") {
            binding.tvDifficultyError.visibility = android.view.View.VISIBLE
            hasError = true
        }
        if (isCompleted == 1 && (completedDate == null || binding.tvCompletedDate.text == "Select completed date")) {
            binding.tvCompletedDateError.visibility = android.view.View.VISIBLE
            hasError = true
        }

        if (hasError) return false

        // Update hike in database
        val length = lengthStr.toDouble()
        val updatedHike = Hike(
            id = currentHikeId,
            name = hikeName,
            location = location,
            date = date,
            parking = parking,
            length = length,
            routeType = if (routeType != "Select route type") routeType else "",
            difficulty = difficulty,
            description = description.ifEmpty { null },
            notes = notes.ifEmpty { null },
            weather = weather.ifEmpty { null },
            isCompleted = isCompleted,
            completedDate = completedDate,
            createdAt = currentHike.createdAt
        )

        val success = dbHelper.updateHike(updatedHike)
        if (success) {
            Toast.makeText(this, "Hike updated successfully!", Toast.LENGTH_SHORT).show()
            // Set result to indicate successful update
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Failed to update hike", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    private fun toggleCompletedStatus(isCompleted: Boolean) {
        this.isCompleted = if (isCompleted) 1 else 0

        // Update completion status text
        val statusText = if (isCompleted) "Marked as completed" else "Mark as planned"
        binding.tvCompletionStatus.text = statusText

        // Update switch colors
        if (isCompleted) {
            binding.switchHikeCompleted.trackTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E6A65"))
            binding.switchHikeCompleted.thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        } else {
            binding.switchHikeCompleted.trackTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#f0f0f0"))
            binding.switchHikeCompleted.thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#f4f3f4"))
        }

        // Show/hide completed date section
        binding.layoutCompletedDateContainer.isVisible = isCompleted

        // Set completed date if not set
        if (isCompleted && completedDate == null) {
            val calendar = Calendar.getInstance()
            val dateText = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
            binding.tvCompletedDate.text = dateText
            completedDate = dateText
            binding.tvCompletedDate.setTextColor(Color.BLACK)
        }

        // Clear completed date error
        binding.tvCompletedDateError.visibility = android.view.View.GONE
    }

    private fun showDatePicker(textView: android.widget.TextView, title: String) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val dateText = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                textView.text = dateText
                textView.setTextColor(Color.BLACK)

                // Store the completed date if this is the completed date picker
                if (textView == binding.tvCompletedDate) {
                    completedDate = dateText
                    binding.tvCompletedDateError.visibility = android.view.View.GONE
                } else if (textView == binding.tvSelectedDate) {
                    binding.tvDateError.visibility = android.view.View.GONE
                }
            }, year, month, day
        )
        datePicker.setTitle(title)
        datePicker.show()
    }
}
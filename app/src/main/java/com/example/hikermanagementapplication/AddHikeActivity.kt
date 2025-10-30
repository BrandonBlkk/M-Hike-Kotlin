package com.example.hikermanagementapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hikermanagementapplication.databinding.ActivityAddHikeBinding
import java.util.Calendar

class AddHikeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHikeBinding
    private lateinit var dbHelper: HikeDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHikeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Parking Spinner
        val parkingOptions = listOf("Select parking availability", "Available", "Not Available", "Limited")
        val parkingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parkingOptions)
        parkingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spParking.adapter = parkingAdapter

        // Difficulty Spinner
        val difficultyOptions = listOf("Select difficulty level", "Easy", "Moderate", "Hard", "Extreme")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficultyOptions)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDifficulty.adapter = difficultyAdapter

        // Hike Status Spinner
        val statusOptions = listOf("Select hike status", "Planned", "Ongoing", "Completed", "Cancelled")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spStatus.adapter = statusAdapter

        // Date Picker
        binding.layoutPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val dateText = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                    binding.tvSelectedDate.text = dateText
                    binding.tvSelectedDate.setTextColor(getColor(android.R.color.black))
                }, year, month, day
            )
            datePicker.show()
        }

        // Submit Button
        binding.btnSubmitHike.setOnClickListener {
            val hikeName = binding.etHikeName.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val parking = binding.spParking.selectedItem.toString()
            val difficulty = binding.spDifficulty.selectedItem.toString()
            val status = binding.spStatus.selectedItem.toString()
            val lengthStr = binding.etLength.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val weather = binding.etWeather.text.toString().trim()
            val trailType = binding.etTrailType.text.toString().trim()
            val date = binding.tvSelectedDate.text.toString().trim()

            // Validation
            if (hikeName.isEmpty() || location.isEmpty() || date.isEmpty() ||
                parking == "Select parking availability" ||
                difficulty == "Select difficulty level" ||
                status == "Select hike status" ||
                lengthStr.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val length = lengthStr.toDoubleOrNull()
            if (length == null) {
                Toast.makeText(this, "Length must be a valid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create Hike object
            val hike = Hike(
                name = hikeName,
                location = location,
                date = date,
                parking = parking,
                length = length,
                difficulty = difficulty,
                status = status,
                description = description.ifEmpty { null },
                trailType = trailType.ifEmpty { null },
                weather = weather.ifEmpty { null }
            )

            val id = dbHelper.insertHike(hike)

            if (id > 0) {
                Toast.makeText(this, "Hike saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save hike", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

package com.example.hikermanagementapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hikermanagementapplication.databinding.ActivityAddObservationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddObservationBinding
    private lateinit var dbHelper: HikeDbHelper
    private var hikeIdMap = mutableMapOf<String, Long>() // map to store hike name → id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Back Button
        binding.btnBackObservation.setOnClickListener {
            finish()
        }

        // Load hike data dynamically
        loadHikeData()

        // Auto-fill current time
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvTime.text = "Current time: $currentTime"

        // Save Observation Button
        binding.btnSaveObservation.setOnClickListener {
            val selectedHikeName = binding.spSelectHike.selectedItem?.toString() ?: ""
            val selectedHikeId = hikeIdMap[selectedHikeName]
            val observationText = binding.etObservation.text.toString().trim()
            val comments = binding.etComments.text.toString().trim()

            if (selectedHikeName == "Select hike" || selectedHikeId == null || observationText.isEmpty()) {
                Toast.makeText(this, "Please select a hike and fill observation", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val observation = Observation(
                hikeId = selectedHikeId,
                observation = observationText,
                obsTime = currentTime,
                comments = comments.ifEmpty { null }
            )

            val result = dbHelper.insertObservation(observation)

            if (result > 0) {
                Toast.makeText(this, "Observation saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save observation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadHikeData() {
        val hikes = dbHelper.getAllHikes()
        val hikeNames = mutableListOf("Select hike")

        // map hike name to id
        for ((index, hike) in hikes.withIndex()) {
            hikeNames.add(hike.name)
            hikeIdMap[hike.name] = (index + 1).toLong() // ensure correct ID mapping
        }

        if (hikes.isEmpty()) {
            Toast.makeText(this, "No hikes available. Please add a hike first.", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hikeNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSelectHike.adapter = adapter
    }
}
package com.example.hikermanagementapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hikermanagementapplication.databinding.ActivityEditObservationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditObservationBinding
    private lateinit var dbHelper: HikeDbHelper
    private var currentObservationId: Long = -1
    private var currentHikeId: Long = -1
    private lateinit var currentObservation: Observation
    private var isNewObservation = false
    private var currentTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Get observation ID and hike ID
        currentObservationId = intent.getLongExtra("observationId", -1L)
        currentHikeId = intent.getLongExtra("hikeId", -1L)

        // Check if this is a new observation
        isNewObservation = currentObservationId == -1L

        if (currentHikeId == -1L) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentTime = getCurrentDateTime()

        if (isNewObservation) {
            // New observation
            setupNewObservation()
        } else {
            // Existing observation
            val observation = getObservationById(currentObservationId)
            if (observation == null) {
                Toast.makeText(this, "Error: Observation not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            currentObservation = observation
            setupForm(observation)
        }

        setupListeners()
    }

    private fun setupNewObservation() {
        // Set current date and time for new observation
        binding.tvCurrentTime.text = currentTime
        binding.tvCurrentTime.setTextColor(resources.getColor(android.R.color.black))

        // Change title and button for new observation
        binding.tvTitle.text = "Add New Observation"
        binding.btnUpdateObservation.text = "Add Observation"
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getObservationById(obsId: Long): Observation? {
        val observations = dbHelper.getObservationsForHike(currentHikeId)
        return observations.find { it.obsId == obsId }
    }

    private fun setupForm(observation: Observation) {
        binding.etObservation.setText(observation.observation)
        binding.etComments.setText(observation.comments ?: "")

        // Set current time for existing observation
        binding.tvCurrentTime.text = currentTime
        binding.tvCurrentTime.setTextColor(resources.getColor(android.R.color.black))
    }

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Update/Add Button
        binding.btnUpdateObservation.setOnClickListener {
            if (isNewObservation) {
                validateAndAddObservation()
            } else {
                validateAndUpdateObservation()
            }
        }
    }

    private fun validateAndAddObservation(): Boolean {
        val observationText = binding.etObservation.text.toString().trim()
        val comments = binding.etComments.text.toString().trim()

        var hasError = false

        // Clear previous errors
        binding.tvObservationError.visibility = android.view.View.GONE

        // Validations
        if (observationText.isEmpty()) {
            binding.tvObservationError.visibility = android.view.View.VISIBLE
            hasError = true
        }

        if (hasError) return false

        // Add new observation to database with current time
        val newObservation = Observation(
            hikeId = currentHikeId,
            observation = observationText,
            obsTime = currentTime,
            comments = comments.ifEmpty { null }
        )

        val newId = dbHelper.insertObservation(newObservation)
        if (newId != -1L) {
            Toast.makeText(this, "Observation added successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Failed to add observation", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    private fun validateAndUpdateObservation(): Boolean {
        val observationText = binding.etObservation.text.toString().trim()
        val comments = binding.etComments.text.toString().trim()

        var hasError = false

        // Clear previous errors
        binding.tvObservationError.visibility = android.view.View.GONE

        // Validations
        if (observationText.isEmpty()) {
            binding.tvObservationError.visibility = android.view.View.VISIBLE
            hasError = true
        }

        if (hasError) return false

        // Update observation in database with current time
        val updatedObservation = Observation(
            obsId = currentObservationId,
            hikeId = currentHikeId,
            observation = observationText,
            obsTime = currentTime,
            comments = comments.ifEmpty { null }
        )

        // Update observation in database
        val success = updateObservation(updatedObservation)
        if (success) {
            Toast.makeText(this, "Observation updated successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Failed to update observation", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    private fun updateObservation(observation: Observation): Boolean {
        // Delete the old observation
        dbHelper.deleteObservation(observation.obsId)

        // Insert the updated observation
        val newId = dbHelper.insertObservation(observation)
        return newId != -1L
    }
}
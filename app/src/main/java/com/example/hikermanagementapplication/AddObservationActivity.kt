package com.example.hikermanagementapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.hikermanagementapplication.databinding.ActivityAddObservationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddObservationBinding
    private lateinit var dbHelper: HikeDbHelper
    private var hikeIdMap = mutableMapOf<String, Long>() // map to store hike name → id
    private var preSelectedHikeId: Long = -1
    private var preSelectedHikeName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Get hike ID and name from intent if coming from AddHikeActivity
        preSelectedHikeId = intent.getLongExtra("HIKE_ID", -1)
        preSelectedHikeName = intent.getStringExtra("HIKE_NAME") ?: ""

        // Back Button
        binding.btnBackObservation.setOnClickListener {
            finish()
        }

        // Set up text change listeners
        setupTextChangeListeners()

        // Set up spinner change listener
        setupSpinnerChangeListener()

        // Load hike data dynamically
        loadHikeData()

        // Auto-fill current time
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvTime.text = "Current time: $currentTime"

        // Save Observation Button
        binding.btnSaveObservation.setOnClickListener {
            saveObservation(currentTime)
        }
    }

    private fun setupTextChangeListeners() {
        // Observation text change listener
        binding.etObservation.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.toString()?.isNotEmpty() == true) {
                    binding.tvObservationError.visibility = android.view.View.GONE
                    updateInputMargin(binding.etObservation, true)
                }
            }
        })

        // Comments text change listener (optional field)
        binding.etComments.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // Comments are optional, no error to clear
            }
        })
    }

    private fun setupSpinnerChangeListener() {
        // Hike spinner change listener
        binding.spSelectHike.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem != "Select hike") {
                    binding.tvHikeError.visibility = android.view.View.GONE
                    updateInputMargin(binding.spSelectHike, true)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun updateInputMargin(view: android.view.View, hasError: Boolean) {
        val layoutParams = view.layoutParams as android.widget.LinearLayout.LayoutParams
        if (hasError) {
            layoutParams.bottomMargin = 4.dpToPx()
        } else {
            layoutParams.bottomMargin = 16.dpToPx()
        }
        view.layoutParams = layoutParams
    }

    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

    private fun loadHikeData() {
        val hikes = dbHelper.getAllHikes()
        val hikeNames = mutableListOf("Select hike")

        // map hike name to id
        for (hike in hikes) {
            hikeNames.add(hike.name)
            hikeIdMap[hike.name] = hike.id
        }

        if (hikes.isEmpty()) {
            Toast.makeText(this, "No hikes available. Please add a hike first.", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hikeNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSelectHike.adapter = adapter

        // If we have a pre-selected hike from AddHikeActivity, select it automatically
        if (preSelectedHikeId != -1L && preSelectedHikeName.isNotEmpty()) {
            val position = adapter.getPosition(preSelectedHikeName)
            if (position >= 0) {
                binding.spSelectHike.setSelection(position)
                // Clear any existing error since we're pre-selecting a valid hike
                binding.tvHikeError.visibility = android.view.View.GONE
                updateInputMargin(binding.spSelectHike, true)
            }
        }
    }

    private fun saveObservation(currentTime: String) {
        val selectedHikeName = binding.spSelectHike.selectedItem?.toString() ?: ""
        val selectedHikeId = if (preSelectedHikeId != -1L) {
            preSelectedHikeId // Use the pre-selected hike ID if available
        } else {
            hikeIdMap[selectedHikeName] ?: -1
        }

        val observationText = binding.etObservation.text.toString().trim()
        val comments = binding.etComments.text.toString().trim()

        var hasError = false

        // Clear previous errors and reset margins
        binding.tvHikeError.visibility = android.view.View.GONE
        binding.tvObservationError.visibility = android.view.View.GONE

        // Reset all input margins to 16dp
        updateInputMargin(binding.spSelectHike, false)
        updateInputMargin(binding.etObservation, false)

        // Validation
        if (selectedHikeName == "Select hike" || selectedHikeId == -1L) {
            binding.tvHikeError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.spSelectHike, true)
            hasError = true
        }

        if (observationText.isEmpty()) {
            binding.tvObservationError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.etObservation, true)
            hasError = true
        }

        if (hasError) return

        val observation = Observation(
            hikeId = selectedHikeId,
            observation = observationText,
            obsTime = currentTime,
            comments = comments.ifEmpty { null }
        )

        val result = dbHelper.insertObservation(observation)

        if (result > 0) {
            Toast.makeText(this, "Observation added successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to add observation", Toast.LENGTH_SHORT).show()
        }
    }
}
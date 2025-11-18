package com.example.hikermanagementapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.hikermanagementapplication.databinding.ActivityAddHikeBinding
import com.example.hikermanagementapplication.databinding.ConfirmationDialogBinding
import java.util.Calendar

class AddHikeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHikeBinding
    private lateinit var dbHelper: HikeDbHelper

    private var isCompleted = 0
    private var completedDate: String? = null
    private var currentHikeId: Long = -1 // Track the current hike ID

    // Store the current hike data for confirmation
    private var currentHikeData: Hike? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHikeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Back button
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, HikeListActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Parking Spinner
        val parkingOptions = listOf("Select parking option", "Yes", "No")
        val parkingAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parkingOptions)
        parkingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spParking.adapter = parkingAdapter

        // Difficulty Spinner
        val difficultyOptions = listOf("Select difficulty level", "Easy", "Moderate", "Hard")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficultyOptions)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spDifficulty.adapter = difficultyAdapter

        // Route Type Spinner
        val routeTypeOptions = listOf("Select route type", "Loop", "Out & Back", "Point to Point", "Lollipop")
        val routeTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeTypeOptions)
        routeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRouteType.adapter = routeTypeAdapter

        // Set up text change listeners
        setupTextChangeListeners()

        // Set up spinner change listeners
        setupSpinnerChangeListeners()

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

        // Add Observation Button
        binding.btnAddObservation.setOnClickListener {
            addObservationForCurrentHike()
        }

        // Submit Button
        binding.btnSubmitHike.setOnClickListener {
            validateAndShowConfirmation()
        }
    }

    private fun validateAndShowConfirmation() {
        if (validateForm()) {
            showCustomConfirmationDialog()
        }
    }

    private fun validateForm(): Boolean {
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

        // Clear previous errors and reset margins
        binding.etHikeName.error = null
        binding.etLocation.error = null
        binding.etLength.error = null
        binding.tvSelectedDate.error = null
        binding.tvHikeNameError.visibility = android.view.View.GONE
        binding.tvLocationError.visibility = android.view.View.GONE
        binding.tvDateError.visibility = android.view.View.GONE
        binding.tvParkingError.visibility = android.view.View.GONE
        binding.tvLengthError.visibility = android.view.View.GONE
        binding.tvDifficultyError.visibility = android.view.View.GONE
        binding.tvCompletedDateError.visibility = android.view.View.GONE

        // Reset all input margins to 16dp
        updateInputMargin(binding.etHikeName, false)
        updateInputMargin(binding.etLocation, false)
        updateInputMargin(binding.etLength, false)
        updateInputMargin(binding.spParking, false)
        updateInputMargin(binding.spDifficulty, false)
        updateInputMargin(binding.layoutPickDate, false)
        updateInputMargin(binding.layoutPickCompletedDate, false)

        // Validations
        if (hikeName.isEmpty()) {
            binding.tvHikeNameError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.etHikeName, true)
            hasError = true
        }
        if (location.isEmpty()) {
            binding.tvLocationError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.etLocation, true)
            hasError = true
        }
        if (date == "Select date" || date.isEmpty()) {
            binding.tvDateError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.layoutPickDate, true)
            hasError = true
        }
        if (parking == "Select parking option") {
            binding.tvParkingError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.spParking, true)
            hasError = true
        }
        if (lengthStr.isEmpty()) {
            binding.tvLengthError.visibility = android.view.View.VISIBLE
            binding.tvLengthError.text = "Length is required"
            updateInputMargin(binding.etLength, true)
            hasError = true
        } else if (lengthStr.toDoubleOrNull() == null || lengthStr.toDouble() <= 0) {
            binding.tvLengthError.visibility = android.view.View.VISIBLE
            binding.tvLengthError.text = "Length must be a valid number"
            updateInputMargin(binding.etLength, true)
            hasError = true
        }
        if (difficulty == "Select difficulty level") {
            binding.tvDifficultyError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.spDifficulty, true)
            hasError = true
        }
        if (isCompleted == 1 && (completedDate == null || binding.tvCompletedDate.text == "Select completed date")) {
            binding.tvCompletedDateError.visibility = android.view.View.VISIBLE
            updateInputMargin(binding.layoutPickCompletedDate, true)
            hasError = true
        }

        if (hasError) return false

        // Store the current hike data for confirmation
        val length = lengthStr.toDouble()
        currentHikeData = Hike(
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
            createdAt = null
        )

        return true
    }

    private fun showCustomConfirmationDialog() {
        val hike = currentHikeData ?: return

        // Inflate custom confirmation dialog layout
        val confirmationBinding = ConfirmationDialogBinding.inflate(LayoutInflater.from(this))

        // Create dialog
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(confirmationBinding.root)
            .setCancelable(false)
            .create()

        // Set dialog window properties
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Populate confirmation data
        populateConfirmationData(confirmationBinding, hike)

        // Button click listeners
        confirmationBinding.btnEditDetails.setOnClickListener {
            dialog.dismiss()
        }

        confirmationBinding.btnConfirmSave.setOnClickListener {
            dialog.dismiss()
            saveHikeToDatabase()
        }

        dialog.show()
    }

    private fun populateConfirmationData(binding: ConfirmationDialogBinding, hike: Hike) {
        // Basic Information Section
        binding.confirmationHikeName.text = hike.name
        binding.confirmationLocation.text = hike.location
        binding.confirmationDate.text = hike.date
        binding.confirmationParking.text = hike.parking

        // Hike Details Section
        binding.confirmationLength.text = "${hike.length} km"
        binding.confirmationRouteType.text = hike.routeType ?: "Not specified"
        binding.confirmationDifficulty.text = hike.difficulty
        binding.confirmationStatus.text = if (hike.isCompleted == 1) "Completed" else "Planned"

        // Completed Date (if applicable)
        if (hike.isCompleted == 1 && !hike.completedDate.isNullOrEmpty()) {
            binding.confirmationCompletedDate.text = hike.completedDate
            binding.confirmationCompletedDateContainer.isVisible = true
        } else {
            binding.confirmationCompletedDateContainer.isVisible = false
        }

        // Description (if available)
        if (!hike.description.isNullOrEmpty()) {
            binding.confirmationDescription.text = hike.description
            binding.confirmationDescriptionContainer.isVisible = true
        } else {
            binding.confirmationDescriptionContainer.isVisible = false
        }

        // Personal Notes (if available)
        if (!hike.notes.isNullOrEmpty()) {
            binding.confirmationNotes.text = hike.notes
            binding.confirmationNotesContainer.isVisible = true
        } else {
            binding.confirmationNotesContainer.isVisible = false
        }

        // Weather Conditions (if available)
        if (!hike.weather.isNullOrEmpty()) {
            binding.confirmationWeather.text = hike.weather
            binding.confirmationWeatherContainer.isVisible = true
        } else {
            binding.confirmationWeatherContainer.isVisible = false
        }
    }

    private fun saveHikeToDatabase() {
        val hike = currentHikeData ?: return

        val id = dbHelper.insertHikeWithExtras(
            hike = hike,
            notes = hike.notes,
            isCompleted = hike.isCompleted,
            completedDate = hike.completedDate
        )

        if (id > 0) {
            currentHikeId = id // Store the hike ID for observation reference

            // Show success message and clear form
            Toast.makeText(this, "Hike added successfully! You can now add observations.", Toast.LENGTH_LONG).show()
            clearForm()

            // Navigate to HikeList activity
            val intent = Intent(this, HikeListActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Failed to add hike", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        // Clear all form fields
        binding.etHikeName.text.clear()
        binding.etLocation.text.clear()
        binding.etLength.text.clear()
        binding.etDescription.text.clear()
        binding.etWeather.text.clear()
        binding.etNotes.text.clear()

        // Reset spinners to default positions
        binding.spParking.setSelection(0)
        binding.spDifficulty.setSelection(0)
        binding.spRouteType.setSelection(0)

        // Reset date pickers
        binding.tvSelectedDate.text = "Select date"
        binding.tvSelectedDate.setTextColor(Color.parseColor("#777777"))
        binding.tvCompletedDate.text = "Select completed date"
        binding.tvCompletedDate.setTextColor(Color.parseColor("#777777"))

        // Reset completion status
        binding.switchHikeCompleted.isChecked = false
        binding.layoutCompletedDateContainer.isVisible = false

        // Clear stored data
        currentHikeData = null
        completedDate = null
        isCompleted = 0
    }

    private fun addObservationForCurrentHike() {
        if (currentHikeId == -1L) {
            Toast.makeText(this, "Please save the hike first before adding observations", Toast.LENGTH_LONG).show()
        } else {
            // Navigate to AddObservationActivity with the current hike ID
            val intent = Intent(this, AddObservationActivity::class.java).apply {
                putExtra("HIKE_ID", currentHikeId)
                putExtra("HIKE_NAME", binding.etHikeName.text.toString().trim())
            }
            startActivity(intent)
        }
    }

    private fun setupTextChangeListeners() {
        // Hike Name text change listener
        binding.etHikeName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.toString()?.isNotEmpty() == true) {
                    binding.tvHikeNameError.visibility = android.view.View.GONE
                    updateInputMargin(binding.etHikeName, false)
                }
            }
        })

        // Location text change listener
        binding.etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.toString()?.isNotEmpty() == true) {
                    binding.tvLocationError.visibility = android.view.View.GONE
                    updateInputMargin(binding.etLocation, false)
                }
            }
        })

        // Length text change listener
        binding.etLength.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s?.toString()?.trim()
                if (text?.isNotEmpty() == true) {
                    val length = text.toDoubleOrNull()
                    if (length != null && length > 0) {
                        binding.tvLengthError.visibility = android.view.View.GONE
                        updateInputMargin(binding.etLength, false)
                    }
                }
            }
        })

        // Weather text change listener
        binding.etWeather.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Description text change listener
        binding.etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Notes text change listener
        binding.etNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupSpinnerChangeListeners() {
        // Parking spinner change listener
        binding.spParking.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem != "Select parking option") {
                    binding.tvParkingError.visibility = android.view.View.GONE
                    updateInputMargin(binding.spParking, false)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Difficulty spinner change listener
        binding.spDifficulty.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                if (selectedItem != "Select difficulty level") {
                    binding.tvDifficultyError.visibility = android.view.View.GONE
                    updateInputMargin(binding.spDifficulty, false)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Route Type spinner change listener (optional field)
        binding.spRouteType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {}
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

        // If marked as completed and no completed date is set, set it to current date
        if (isCompleted && completedDate == null) {
            val calendar = Calendar.getInstance()
            val dateText = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
            binding.tvCompletedDate.text = dateText
            completedDate = dateText
            binding.tvCompletedDate.setTextColor(Color.BLACK)
        }

        // Clear completed date error when toggle is changed
        binding.tvCompletedDateError.visibility = android.view.View.GONE
        updateInputMargin(binding.layoutPickCompletedDate, false)
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
                    // Clear error when date is selected
                    binding.tvCompletedDateError.visibility = android.view.View.GONE
                    updateInputMargin(binding.layoutPickCompletedDate, false)
                } else if (textView == binding.tvSelectedDate) {
                    // Clear error when hike date is selected
                    binding.tvDateError.visibility = android.view.View.GONE
                    updateInputMargin(binding.layoutPickDate, false)
                }
            }, year, month, day
        )
        datePicker.setTitle(title)
        datePicker.show()
    }
}
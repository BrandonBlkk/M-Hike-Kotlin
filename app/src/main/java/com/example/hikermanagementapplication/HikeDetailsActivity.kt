package com.example.hikermanagementapplication

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hikermanagementapplication.databinding.ActivityHikeDetailsBinding
import com.example.hikermanagementapplication.databinding.ItemObservationBinding

class HikeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHikeDetailsBinding
    private lateinit var dbHelper: HikeDbHelper
    private var currentHikeId: Long = -1L
    private lateinit var currentHike: Hike

    // Register for activity result to refresh data after editing
    private val editHikeResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh the hike details after successful edit
            loadHikeDetails()
        }
    }

    private val editObservationResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh the observations after successful edit
            loadHikeDetails()
        }
    }

    private val addObservationResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh the observations after adding new one
            loadHikeDetails()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHikeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Setup back button
        setupBackButton()

        // Get hike ID from intent
        currentHikeId = intent.getLongExtra("hikeId", -1L)

        if (currentHikeId == -1L) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load initial hike details
        loadHikeDetails()
    }

    private fun loadHikeDetails() {
        // Load hike details
        val hike = dbHelper.getHikeById(currentHikeId)
        if (hike == null) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentHike = hike

        // Load observations for this hike
        val observations = dbHelper.getObservationsByHikeId(currentHikeId)

        setupViews(hike, observations)
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupViews(hike: Hike, observations: List<Observation>) {
        // Set hike information
        binding.hikeNameText.text = hike.name
        binding.hikeLocationText.text = hike.location
        binding.hikeDateText.text = hike.date
        binding.hikeLengthText.text = "${hike.length} km"
        binding.hikeDifficultyText.text = hike.difficulty
        binding.hikeParkingText.text = hike.parking
        binding.hikeRouteTypeText.text = hike.routeType ?: "Not specified"

        // Set status with dynamic background color
        val statusText = if (hike.isCompleted == 1) "COMPLETED" else "PLANNED"
        binding.hikeStatusText.text = statusText

        // Set background based on status
        if (hike.isCompleted == 1) {
            binding.hikeStatusText.setBackgroundResource(R.drawable.status_completed_bg)
        } else {
            binding.hikeStatusText.setBackgroundResource(R.drawable.status_planned_bg)
        }

        // Show/hide completed date
        if (hike.isCompleted == 1 && !hike.completedDate.isNullOrEmpty()) {
            binding.completedDateLayout.visibility = View.VISIBLE
            binding.hikeCompletedDateText.text = hike.completedDate
        } else {
            binding.completedDateLayout.visibility = View.GONE
        }

        // Set description and notes
        binding.hikeDescriptionText.text = hike.description ?: "No description provided"
        binding.hikeNotesText.text = hike.notes ?: "No notes provided"

        // Setup observations
        setupObservations(observations)

        // Add Observation button
        binding.btnAddObservation.setOnClickListener {
            addNewObservation()
        }

        // Edit button - UPDATED to use the launcher
        binding.editHikeButton.setOnClickListener {
            val intent = Intent(this, EditHikeActivity::class.java).apply {
                putExtra("hikeId", hike.id)
            }
            editHikeResultLauncher.launch(intent)
        }

        // Delete button
        binding.deleteHikeButton.setOnClickListener {
            showDeleteConfirmationDialog(hike.id)
        }
    }

    private fun addNewObservation() {
        val intent = Intent(this, AddObservationActivity::class.java).apply {
            putExtra("HIKE_ID", currentHikeId)
            putExtra("HIKE_NAME", currentHike.name)
        }
        addObservationResultLauncher.launch(intent)
    }

    private fun setupObservations(observations: List<Observation>) {
        // Update count
        binding.observationsCountText.text = "${observations.size}"

        if (observations.isEmpty()) {
            binding.noObservationsLayout.visibility = View.VISIBLE
            binding.observationsListLayout.visibility = View.GONE
        } else {
            binding.noObservationsLayout.visibility = View.GONE
            binding.observationsListLayout.visibility = View.VISIBLE

            // Clear existing views
            binding.observationsListLayout.removeAllViews()

            // Add each observation
            observations.forEachIndexed { index, observation ->
                val observationView = createObservationView(observation, index)
                binding.observationsListLayout.addView(observationView)

                // Add divider between observations (except for the last one)
                if (index < observations.size - 1) {
                    val divider = View(this)
                    divider.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        setMargins(0, 12, 0, 12)
                    }
                    divider.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                    binding.observationsListLayout.addView(divider)
                }
            }
        }
    }

    private fun createObservationView(observation: Observation, index: Int): View {
        val observationBinding = ItemObservationBinding.inflate(LayoutInflater.from(this))

        observationBinding.observationText.text = observation.observation
        observationBinding.observationTime.text = "Time: ${observation.obsTime}"
        observationBinding.observationComments.text = observation.comments ?: "No additional comments"

        // Edit observation button
        observationBinding.btnEditObservation.setOnClickListener {
            val intent = Intent(this, EditObservationActivity::class.java).apply {
                putExtra("observationId", observation.obsId)
                putExtra("hikeId", currentHikeId)
            }
            editObservationResultLauncher.launch(intent)
        }

        // Delete observation button
        observationBinding.btnDeleteObservation.setOnClickListener {
            showDeleteObservationConfirmationDialog(observation.obsId, observation.observation)
        }

        return observationBinding.root
    }

    private fun showDeleteObservationConfirmationDialog(obsId: Long, observationText: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Observation")
            .setMessage("Are you sure you want to delete this observation?\n\n\"$observationText\"\n\nThis action cannot be undone!")
            .setPositiveButton("Delete") { dialogInterface: DialogInterface, i: Int ->
                deleteObservation(obsId)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            // Red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)

            // Blue
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLUE)
        }

        dialog.show()
    }

    private fun deleteObservation(obsId: Long) {
        dbHelper.deleteObservation(obsId)
        Toast.makeText(this, "Observation deleted successfully", Toast.LENGTH_SHORT).show()
        // Refresh the observations
        loadHikeDetails()
    }

    private fun showDeleteConfirmationDialog(hikeId: Long) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Hike")
            .setMessage("Are you sure you want to delete this hike? This will also delete all associated observations. This action cannot be undone!")
            .setPositiveButton("Delete") { dialogInterface: DialogInterface, i: Int ->
                deleteHike(hikeId)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            // Red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)

            // Blue
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLUE)
        }

        dialog.show()
    }

    private fun deleteHike(hikeId: Long) {
        val deleted = dbHelper.deleteHike(hikeId)
        if (deleted) {
            Toast.makeText(this, "Hike deleted successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error deleting hike", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        if (currentHikeId != -1L) {
            loadHikeDetails()
        }
    }
}
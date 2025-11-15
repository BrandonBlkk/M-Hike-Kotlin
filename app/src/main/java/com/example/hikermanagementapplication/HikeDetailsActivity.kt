package com.example.hikermanagementapplication

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HikeDetailsActivity : AppCompatActivity() {

    private lateinit var dbHelper: HikeDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hike_details)

        dbHelper = HikeDbHelper(this)

        // Setup back button
        setupBackButton()

        // Get hike ID from intent
        val hikeId = intent.getLongExtra("hikeId", -1L)

        if (hikeId == -1L) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load hike details
        val hike = dbHelper.getHikeById(hikeId)
        if (hike == null) {
            Toast.makeText(this, "Error: Hike not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load observations for this hike
        val observations = dbHelper.getObservationsByHikeId(hikeId)

        setupViews(hike, observations)
    }

    private fun setupBackButton() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupViews(hike: Hike, observations: List<Observation>) {
        // Set hike information
        findViewById<TextView>(R.id.hikeNameText).text = hike.name
        findViewById<TextView>(R.id.hikeLocationText).text = hike.location
        findViewById<TextView>(R.id.hikeDateText).text = hike.date
        findViewById<TextView>(R.id.hikeLengthText).text = "${hike.length} km"
        findViewById<TextView>(R.id.hikeDifficultyText).text = hike.difficulty
        findViewById<TextView>(R.id.hikeParkingText).text = hike.parking
        findViewById<TextView>(R.id.hikeRouteTypeText).text = hike.routeType ?: "Not specified"

        // Set status with dynamic background color
        val statusTextView = findViewById<TextView>(R.id.hikeStatusText)
        val statusText = if (hike.isCompleted == 1) "COMPLETED" else "PLANNED"
        statusTextView.text = statusText

        // Set background based on status
        if (hike.isCompleted == 1) {
            statusTextView.setBackgroundResource(R.drawable.status_completed_bg)
        } else {
            statusTextView.setBackgroundResource(R.drawable.status_planned_bg)
        }

        // Show/hide completed date
        if (hike.isCompleted == 1 && !hike.completedDate.isNullOrEmpty()) {
            findViewById<LinearLayout>(R.id.completedDateLayout).visibility = View.VISIBLE
            findViewById<TextView>(R.id.hikeCompletedDateText).text = hike.completedDate
        } else {
            findViewById<LinearLayout>(R.id.completedDateLayout).visibility = View.GONE
        }

        // Set description and notes
        findViewById<TextView>(R.id.hikeDescriptionText).text = hike.description ?: "No description provided"
        findViewById<TextView>(R.id.hikeNotesText).text = hike.notes ?: "No notes provided"

        // Setup observations
        setupObservations(observations)

        // Delete button
        findViewById<Button>(R.id.deleteHikeButton).setOnClickListener {
            showDeleteConfirmationDialog(hike.id)
        }
    }

    private fun setupObservations(observations: List<Observation>) {
        val observationsCountText = findViewById<TextView>(R.id.observationsCountText)
        val observationsListLayout = findViewById<LinearLayout>(R.id.observationsListLayout)
        val noObservationsLayout = findViewById<LinearLayout>(R.id.noObservationsLayout)

        // Update count
        observationsCountText.text = "${observations.size}"

        if (observations.isEmpty()) {
            noObservationsLayout.visibility = View.VISIBLE
            observationsListLayout.visibility = View.GONE
        } else {
            noObservationsLayout.visibility = View.GONE
            observationsListLayout.visibility = View.VISIBLE

            // Clear existing views
            observationsListLayout.removeAllViews()

            // Add each observation
            observations.forEachIndexed { index, observation ->
                val observationView = createObservationView(observation, index)
                observationsListLayout.addView(observationView)

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
                    observationsListLayout.addView(divider)
                }
            }
        }
    }

    private fun createObservationView(observation: Observation, index: Int): View {
        val inflater = LayoutInflater.from(this)
        val observationView = inflater.inflate(R.layout.item_observation, null) as LinearLayout

        val observationText = observationView.findViewById<TextView>(R.id.observationText)
        val observationTime = observationView.findViewById<TextView>(R.id.observationTime)
        val observationComments = observationView.findViewById<TextView>(R.id.observationComments)

        observationText.text = observation.observation
        observationTime.text = "Time: ${observation.obsTime}"
        observationComments.text = observation.comments ?: "No additional comments"

        return observationView
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
}
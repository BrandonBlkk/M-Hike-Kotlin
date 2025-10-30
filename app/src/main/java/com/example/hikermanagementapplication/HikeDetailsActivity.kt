package com.example.hikermanagementapplication

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
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

        val hikeNameText: TextView = findViewById(R.id.hikeNameText)
        val hikeDateText: TextView = findViewById(R.id.hikeDateText)
        val hikeDistanceText: TextView = findViewById(R.id.hikeDistanceText)
        val hikeNotesText: TextView = findViewById(R.id.hikeNotesText)
        val deleteButton: Button = findViewById(R.id.deleteHikeButton)

        // Get data from intent
        val hikeId = intent.getLongExtra("hikeId", -1L)
        val hikeName = intent.getStringExtra("hikeName")
        val hikeDate = intent.getStringExtra("hikeDate")
        val hikeDistance = intent.getDoubleExtra("hikeDistance", 0.0)
        val hikeNotes = intent.getStringExtra("hikeNotes")

        // Set data to views
        hikeNameText.text = hikeName
        hikeDateText.text = hikeDate
        hikeDistanceText.text = "$hikeDistance km"
        hikeNotesText.text = hikeNotes

        // Delete button click
        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Hike")
                .setMessage("Are you sure you want to delete this hike?")
                .setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                    if (hikeId != -1L) {
                        val deleted = dbHelper.deleteHike(hikeId)
                        if (deleted) {
                            Toast.makeText(this, "Hike deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Error deleting hike", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
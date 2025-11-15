package com.example.hikermanagementapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hikermanagementapplication.databinding.ActivityHikeListBinding
import com.example.hikermanagementapplication.databinding.DialogAdvancedSearchBinding

class HikeListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHikeListBinding
    private lateinit var hikeAdapter: HikeListAdapter
    private lateinit var hikeList: MutableList<Hike>
    private lateinit var filteredHikeList: MutableList<Hike>
    private lateinit var dbHelper: HikeDbHelper

    // Advanced search filters
    private var currentSearchQuery: String = ""
    private var currentLocationFilter: String = ""
    private var currentMinLength: Double? = null
    private var currentMaxLength: Double? = null
    private var currentDateFilter: String = ""

    companion object {
        private const val TAG = "HikeListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHikeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)

        // Initialize lists
        hikeList = mutableListOf()
        filteredHikeList = mutableListOf()

        setupRecyclerView()
        setupSearchFunctionality()
        setupBottomNavigation()
        loadHikes()

        // Set up clear search button from empty state
        binding.clearSearchButtonEmpty.setOnClickListener {
            onClearSearchClicked(it)
        }
    }

    private fun setupRecyclerView() {
        hikeAdapter = HikeListAdapter(filteredHikeList, dbHelper, this)

        binding.hikesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HikeListActivity)
            adapter = hikeAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyAdvancedFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Advanced search button
        binding.clearSearchButton.setImageResource(R.drawable.eye_line)
        binding.clearSearchButton.visibility = View.VISIBLE
        binding.clearSearchButton.setOnClickListener {
            showAdvancedSearchDialog()
        }
    }

    private fun setupBottomNavigation() {
        // Set the My Hikes item as selected
        binding.bottomNavigation.selectedItemId = R.id.navMyHike

        // Refresh list
        val refreshLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            loadHikes()
        }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    val intent = Intent(this, MainActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
                R.id.navMyHike -> {
                    loadHikes()
                    true
                }
                R.id.navAddHike -> {
                    val intent = Intent(this, AddHikeActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
                R.id.navMap -> {
                    val intent = Intent(this, AddObservationActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
                R.id.navWeather -> {
                    val intent = Intent(this, WeatherForecastActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadHikes() {
        showLoadingState(true)

        try {
            val hikes = dbHelper.getAllHikes()
            Log.d(TAG, "Loaded ${hikes.size} hikes from database")

            hikeList.clear()
            hikeList.addAll(hikes)

            filteredHikeList.clear()
            filteredHikeList.addAll(hikes)

            hikeAdapter.notifyDataSetChanged()
            updateHikeCount()
            updateEmptyState()

            // Debug logging
            if (hikes.isEmpty()) {
                Log.d(TAG, "No hikes found in database")
            } else {
                hikes.forEach { hike ->
                    Log.d(TAG, "Hike: ${hike.name}, ID: ${hike.id}, Location: ${hike.location}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading hikes", e)
            Toast.makeText(this, "Error loading hikes: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            showLoadingState(false)
        }
    }

    private fun applyAdvancedFilters() {
        filteredHikeList.clear()

        if (currentSearchQuery.isEmpty() &&
            currentLocationFilter.isEmpty() &&
            currentMinLength == null &&
            currentMaxLength == null &&
            currentDateFilter.isEmpty()) {

            // Show all hikes
            filteredHikeList.addAll(hikeList)
        } else {
            val filtered = hikeList.filter { hike ->
                // Name search (partial match)
                val nameMatches = currentSearchQuery.isEmpty() ||
                        hike.name.lowercase().contains(currentSearchQuery.lowercase())

                // Location filter (exact or partial match)
                val locationMatches = currentLocationFilter.isEmpty() ||
                        hike.location.lowercase().contains(currentLocationFilter.lowercase())

                // Length range filter
                val lengthMatches = when {
                    currentMinLength != null && currentMaxLength != null ->
                        hike.length >= currentMinLength!! && hike.length <= currentMaxLength!!
                    currentMinLength != null -> hike.length >= currentMinLength!!
                    currentMaxLength != null -> hike.length <= currentMaxLength!!
                    else -> true
                }

                // Date filter (partial match for day, month, or year)
                val dateMatches = currentDateFilter.isEmpty() ||
                        hike.date.contains(currentDateFilter)

                nameMatches && locationMatches && lengthMatches && dateMatches
            }
            filteredHikeList.addAll(filtered)
        }

        hikeAdapter.notifyDataSetChanged()
        updateHikeCount()
        updateEmptyState()
        updateFilterIndicator()
    }

    private fun showAdvancedSearchDialog() {
        val dialogBinding = DialogAdvancedSearchBinding.inflate(layoutInflater)
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Filter your results")
            .setView(dialogBinding.root)
            .setPositiveButton("Apply Filters") { dialog, which ->
                // Get filter values from dialog using binding
                currentLocationFilter = dialogBinding.etLocationFilter.text.toString().trim()
                currentMinLength = dialogBinding.etMinLength.text.toString().trim().toDoubleOrNull()
                currentMaxLength = dialogBinding.etMaxLength.text.toString().trim().toDoubleOrNull()
                currentDateFilter = dialogBinding.etDateFilter.text.toString().trim()

                applyAdvancedFilters()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setNeutralButton("Clear All") { dialog, which ->
                clearAllFilters()
            }
            .create()

        // Pre-fill current filter values
        dialogBinding.etLocationFilter.setText(currentLocationFilter)
        dialogBinding.etMinLength.setText(currentMinLength?.toString() ?: "")
        dialogBinding.etMaxLength.setText(currentMaxLength?.toString() ?: "")
        dialogBinding.etDateFilter.setText(currentDateFilter)

        dialog.setOnShowListener {
            // Blue
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLUE)

            // Red
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)

            // Blue
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.BLUE)
        }

        dialog.show()
    }

    private fun clearAllFilters() {
        currentSearchQuery = ""
        currentLocationFilter = ""
        currentMinLength = null
        currentMaxLength = null
        currentDateFilter = ""

        binding.searchInput.text.clear()
        applyAdvancedFilters()
        updateFilterIndicator()
    }

    private fun updateFilterIndicator() {
        val hasActiveFilters = currentLocationFilter.isNotEmpty() ||
                currentMinLength != null ||
                currentMaxLength != null ||
                currentDateFilter.isNotEmpty()

        if (hasActiveFilters) {
            binding.clearSearchButton.setColorFilter(resources.getColor(android.R.color.holo_red_light))
            binding.clearSearchButton.contentDescription = "Clear all filters"
        } else {
            binding.clearSearchButton.clearColorFilter()
            binding.clearSearchButton.contentDescription = "Advanced search"
        }
    }

    private fun updateHikeCount() {
        val totalHikes = hikeList.size
        val filteredHikes = filteredHikeList.size
        val hasFilters = currentSearchQuery.isNotEmpty() ||
                currentLocationFilter.isNotEmpty() ||
                currentMinLength != null ||
                currentMaxLength != null ||
                currentDateFilter.isNotEmpty()

        val countText = if (hasFilters) {
            "$filteredHikes hike${if (filteredHikes != 1) "s" else ""} found ($totalHikes total)"
        } else {
            "$totalHikes hike${if (totalHikes != 1) "s" else ""} recorded"
        }

        binding.hikeCountText.text = countText
    }

    private fun updateEmptyState() {
        val hasFilters = currentSearchQuery.isNotEmpty() ||
                currentLocationFilter.isNotEmpty() ||
                currentMinLength != null ||
                currentMaxLength != null ||
                currentDateFilter.isNotEmpty()

        if (filteredHikeList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.hikesRecyclerView.visibility = View.GONE

            if (hasFilters) {
                // Search results empty
                binding.emptyStateIcon.setImageResource(R.drawable.search_line)
                binding.emptyStateTitle.text = "No Matching Hikes"
                binding.emptyStateText.text = getEmptyStateFilterMessage()
                binding.clearSearchButtonEmpty.visibility = View.VISIBLE
            } else {
                // No hikes at all
                binding.emptyStateIcon.setImageResource(R.drawable.search_line)
                binding.emptyStateTitle.text = "No Hikes Yet"
                binding.emptyStateText.text = "Start by adding your first hike using the \"Add Hike\" tab!"
                binding.clearSearchButtonEmpty.visibility = View.GONE
            }
        } else {
            binding.emptyState.visibility = View.GONE
            binding.hikesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun getEmptyStateFilterMessage(): String {
        val filters = mutableListOf<String>()

        if (currentSearchQuery.isNotEmpty()) {
            filters.add("name containing \"$currentSearchQuery\"")
        }
        if (currentLocationFilter.isNotEmpty()) {
            filters.add("location containing \"$currentLocationFilter\"")
        }
        if (currentMinLength != null || currentMaxLength != null) {
            val lengthFilter = when {
                currentMinLength != null && currentMaxLength != null ->
                    "length between ${currentMinLength}km and ${currentMaxLength}km"
                currentMinLength != null -> "length ≥ ${currentMinLength}km"
                currentMaxLength != null -> "length ≤ ${currentMaxLength}km"
                else -> ""
            }
            filters.add(lengthFilter)
        }
        if (currentDateFilter.isNotEmpty()) {
            filters.add("date containing \"$currentDateFilter\"")
        }

        return if (filters.isNotEmpty()) {
            "No hikes found with: ${filters.joinToString(", ")}"
        } else {
            "No hikes found with current filters"
        }
    }

    private fun showLoadingState(show: Boolean) {
        if (show) {
            binding.loadingState.visibility = View.VISIBLE
            binding.hikesRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
        } else {
            binding.loadingState.visibility = View.GONE
            updateEmptyState()
        }
    }

    // Handle clear search from empty state
    fun onClearSearchClicked(view: View) {
        clearAllFilters()
    }

    override fun onResume() {
        super.onResume()
        loadHikes() // Refresh the list when returning to this activity
    }
}
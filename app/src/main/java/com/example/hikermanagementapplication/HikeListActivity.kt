package com.example.hikermanagementapplication

import android.content.Intent
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

class HikeListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHikeListBinding
    private lateinit var hikeAdapter: HikeListAdapter
    private lateinit var hikeList: MutableList<Hike>
    private lateinit var filteredHikeList: MutableList<Hike>
    private lateinit var dbHelper: HikeDbHelper

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
                filterHikes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.text.clear()
            filterHikes("")
            binding.clearSearchButton.visibility = View.GONE
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

    private fun filterHikes(query: String) {
        filteredHikeList.clear()

        if (query.isEmpty()) {
            filteredHikeList.addAll(hikeList)
            binding.clearSearchButton.visibility = View.GONE
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            val filtered = hikeList.filter { hike ->
                hike.name.lowercase().contains(lowerCaseQuery) ||
                        hike.location.lowercase().contains(lowerCaseQuery) ||
                        hike.difficulty.lowercase().contains(lowerCaseQuery) ||
                        hike.length.toString().contains(lowerCaseQuery) ||
                        hike.parking.lowercase().contains(lowerCaseQuery)
            }
            filteredHikeList.addAll(filtered)
            binding.clearSearchButton.visibility = View.VISIBLE
        }

        hikeAdapter.notifyDataSetChanged()
        updateHikeCount()
        updateEmptyState()
    }

    private fun updateHikeCount() {
        val totalHikes = hikeList.size
        val filteredHikes = filteredHikeList.size
        val searchQuery = binding.searchInput.text.toString().trim()

        val countText = if (searchQuery.isNotEmpty()) {
            "$filteredHikes hike${if (filteredHikes != 1) "s" else ""} found ($totalHikes total)"
        } else {
            "$totalHikes hike${if (totalHikes != 1) "s" else ""} recorded"
        }

        binding.hikeCountText.text = countText
    }

    private fun updateEmptyState() {
        val searchQuery = binding.searchInput.text.toString().trim()

        if (filteredHikeList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.hikesRecyclerView.visibility = View.GONE

            if (searchQuery.isNotEmpty()) {
                // Search results empty
                binding.emptyStateIcon.setImageResource(R.drawable.search_line)
                binding.emptyStateTitle.text = "No Matching Hikes"
                binding.emptyStateText.text = "No hikes found matching \"$searchQuery\""
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
        binding.searchInput.text.clear()
        filterHikes("")
        binding.clearSearchButton.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadHikes() // Refresh the list when returning to this activity
    }
}
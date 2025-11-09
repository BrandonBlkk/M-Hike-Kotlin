package com.example.hikermanagementapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hikermanagementapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var hikeAdapter: HikeAdapter
    private lateinit var hikeList: MutableList<Hike>
    private lateinit var dbHelper: HikeDbHelper

    // Open Add Hike Screen
    private fun openAddHikeScreen() {
        val intent = Intent(this, AddHikeActivity::class.java)
        startActivity(intent)
    }

    // Open Hike List Screen
    private fun openHikeListScreen() {
        val intent = Intent(this, HikeListActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = HikeDbHelper(this)
        hikeList = dbHelper.getAllHikes().toMutableList()

        hikeAdapter = HikeAdapter(hikeList, dbHelper, this)
        binding.recentHikesRecyclerView.apply {
            // RecyclerView scroll horizontally
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = hikeAdapter
        }

        // Cards Click Listeners
        binding.addHikeCard.setOnClickListener { openAddHikeScreen() }
        binding.recordHikeCard.setOnClickListener { openHikeListScreen() }
        binding.myHikeCard.setOnClickListener { openHikeListScreen() }
        binding.trackProgressCard.setOnClickListener { openHikeListScreen() }

        // View All Button
        binding.viewAllButton.setOnClickListener { openHikeListScreen() }

        // Refresh list
        val refreshLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            refreshHikeList()
        }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> true
                R.id.navMyHike -> {
                    val intent = Intent(this, HikeListActivity::class.java)
                    refreshLauncher.launch(intent)
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
                R.id.navAbout -> {
                    val intent = Intent(this, AddHikeActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun refreshHikeList() {
        hikeList = dbHelper.getAllHikes().toMutableList()
        hikeAdapter.updateData(hikeList)
    }

    override fun onResume() {
        super.onResume()
        refreshHikeList()
    }
}

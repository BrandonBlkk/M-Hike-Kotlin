package com.example.hikermanagementapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AboutScreenActivity : AppCompatActivity() {

    private val refreshLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Handle any result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_screen)

        setupClickListeners()
        setupBottomNavigation()
        displayVersionInfo()
    }

    private fun setupClickListeners() {
        // Share button
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnShare).setOnClickListener {
            shareApp()
        }

        // Info links
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.layoutWebsite).setOnClickListener {
            openWebsite()
        }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.layoutPrivacy).setOnClickListener {
            openPrivacyPolicy()
        }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.layoutTerms).setOnClickListener {
            openTermsOfService()
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@hikerapp.com")
                putExtra(Intent.EXTRA_SUBJECT, "Hiker Management App Feedback")
                putExtra(Intent.EXTRA_TEXT, "Hello Hiker Team,\n\nI would like to share some feedback about the app:\n\n")
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rateApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            }
            startActivity(intent)
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this Hiker Management App")
            putExtra(Intent.EXTRA_TEXT, "I've been using this amazing Hiker Management App to track my hiking adventures! Download it now: https://play.google.com/store/apps/details?id=$packageName")
        }
        startActivity(Intent.createChooser(shareIntent, "Share App"))
    }

    private fun openWebsite() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.hikerapp.com")
        }
        startActivity(intent)
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.hikerapp.com/privacy")
        }
        startActivity(intent)
    }

    private fun openTermsOfService() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.hikerapp.com/terms")
        }
        startActivity(intent)
    }

    private fun displayVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            findViewById<TextView>(R.id.tvVersion).text = "Version $versionName"
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvVersion).text = "Version 1.0.0"
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = R.id.navAbout

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    val intent = Intent(this, MainActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
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
                R.id.navWeather -> {
                    val intent = Intent(this, WeatherForecastActivity::class.java)
                    refreshLauncher.launch(intent)
                    true
                }
                R.id.navAbout -> {
                    // Already on About screen
                    true
                }
                else -> false
            }
        }
    }
}
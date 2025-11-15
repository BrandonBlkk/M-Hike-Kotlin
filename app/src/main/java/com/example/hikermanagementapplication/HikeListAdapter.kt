package com.example.hikermanagementapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hikermanagementapplication.databinding.HikeItemBinding

class HikeListAdapter(
    private var hikeList: MutableList<Hike>,
    private val dbHelper: HikeDbHelper,
    private val context: Context
) : RecyclerView.Adapter<HikeListAdapter.HikeViewHolder>() {

    inner class HikeViewHolder(private val binding: HikeItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hike: Hike) {
            // Display main info
            binding.hikeName.text = hike.name
            binding.hikeLocation.text = hike.location
            binding.hikeDate.text = hike.date
            binding.hikeLength.text = "${hike.length} km"
            binding.hikeDifficulty.text = hike.difficulty
            binding.hikeParking.text = hike.parking

            // For vertical list, show observations and details icon
            binding.observationsContainer.visibility = View.VISIBLE
            binding.viewDetailsIcon.visibility = View.VISIBLE

            // Observations section
            binding.observationsContainer.removeAllViews()
            val observations = dbHelper.getObservationsForHike(hike.id)
            if (observations.isEmpty()) {
                val tv = TextView(binding.root.context).apply {
                    text = "No observations yet"
                    textSize = 12f
                    setTextColor(Color.parseColor("#666666"))
                }
                binding.observationsContainer.addView(tv)
            } else {
                for (obs in observations.take(3)) {
                    val tv = TextView(binding.root.context).apply {
                        text = "• ${obs.observation} (${obs.obsTime})"
                        textSize = 12f
                        setTextColor(Color.parseColor("#666666"))
                        setPadding(0, 4, 0, 4)
                    }
                    binding.observationsContainer.addView(tv)
                }

                if (observations.size > 3) {
                    val tv = TextView(binding.root.context).apply {
                        text = "• View ${observations.size - 3} more observations..."
                        textSize = 12f
                        setTextColor(Color.parseColor("#2196F3"))
                        setPadding(0, 4, 0, 4)
                    }
                    binding.observationsContainer.addView(tv)
                }
            }

            // Set difficulty background color
            val difficultyColor = when (hike.difficulty.lowercase()) {
                "easy" -> Color.parseColor("#4CAF50")
                "moderate" -> Color.parseColor("#FFC107")
                "hard" -> Color.parseColor("#F44336")
                else -> Color.parseColor("#607D8B")
            }
            binding.hikeDifficulty.setBackgroundColor(difficultyColor)
            binding.hikeDifficulty.setPadding(16, 8, 16, 8)
            binding.hikeDifficulty.setTextColor(Color.WHITE)

            // Click listener to open details
            binding.root.setOnClickListener {
                openHikeDetails(hike)
            }

            binding.viewDetailsIcon.setOnClickListener {
                openHikeDetails(hike)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val binding = HikeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HikeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        val hike = hikeList[position]
        holder.bind(hike)
    }

    private fun openHikeDetails(hike: Hike) {
        val intent = Intent(context, HikeDetailsActivity::class.java).apply {
            putExtra("hikeId", hike.id)
            putExtra("hikeName", hike.name)
            putExtra("hikeDate", hike.date)
            putExtra("hikeDistance", hike.length)
            putExtra("hikeLocation", hike.location)
            putExtra("hikeDifficulty", hike.difficulty)
            putExtra("hikeDescription", hike.description)
            putExtra("hikeNotes", hike.notes)
            putExtra("hikeWeather", hike.weather)
            putExtra("hikeRouteType", hike.routeType)
            putExtra("isCompleted", hike.isCompleted)
            putExtra("completedDate", hike.completedDate)
        }
        context.startActivity(intent)
    }

    override fun getItemCount(): Int = hikeList.size

    fun updateData(newList: List<Hike>) {
        hikeList.clear()
        hikeList.addAll(newList)
        notifyDataSetChanged()
    }
}
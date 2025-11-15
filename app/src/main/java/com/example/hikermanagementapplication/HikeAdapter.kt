package com.example.hikermanagementapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hikermanagementapplication.databinding.RecentHikeBinding

class HikeAdapter(
    private var hikeList: MutableList<Hike>,
    private val dbHelper: HikeDbHelper,
    private val context: Context
) : RecyclerView.Adapter<HikeAdapter.HikeViewHolder>() {

    inner class HikeViewHolder(private val binding: RecentHikeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hike: Hike) {
            // Display main info
            binding.hikeName.text = hike.name
            binding.hikeLocation.text = "${hike.location}"
            binding.hikeDistance.text = "${hike.length} km"
            binding.hikeParking.text = hike.parking
            binding.hikeDifficulty.text = hike.difficulty
            binding.hikeDate.text = hike.date

            // Highlight difficulty color
            when (hike.difficulty.lowercase()) {
                "easy" -> binding.hikeDifficulty.setBackgroundColor(Color.parseColor("#4CAF50")) // green
                "moderate" -> binding.hikeDifficulty.setBackgroundColor(Color.parseColor("#FFC107")) // amber
                "hard" -> binding.hikeDifficulty.setBackgroundColor(Color.parseColor("#F44336")) // red
                else -> binding.hikeDifficulty.setBackgroundColor(Color.parseColor("#607D8B")) // grey
            }

            // Observations section
            binding.observationsContainer.removeAllViews()
            val observations = dbHelper.getObservationsForHike(hike.id)
            if (observations.isEmpty()) {
                val tv = TextView(binding.root.context).apply {
                    text = "No observations yet"
                    textSize = 12f
                    setTextColor(Color.parseColor("#333333"))
                }
                binding.observationsContainer.addView(tv)
            } else {
                for (obs in observations) {
                    val tv = TextView(binding.root.context).apply {
                        text = "• ${obs.observation} (${obs.obsTime})"
                        textSize = 12f
                        setTextColor(Color.parseColor("#333333"))
                        setPadding(0, 4, 0, 4)
                    }
                    binding.observationsContainer.addView(tv)
                }
            }

            // Click listener to open details
            binding.viewDetailsIcon.setOnClickListener {
                val intent = Intent(context, HikeDetailsActivity::class.java).apply {
                    putExtra("hikeId", hike.id)
                    putExtra("hikeName", hike.name)
                    putExtra("hikeDate", hike.date)
                    putExtra("hikeDistance", hike.length)
                    putExtra("hikeLocation", hike.location)
                    putExtra("hikeDifficulty", hike.difficulty)
                    putExtra("hikeParking", hike.parking)
                    putExtra("hikeDescription", hike.description)
                    putExtra("hikeNotes", hike.notes)
                    putExtra("hikeWeather", hike.weather)
                    putExtra("hikeRouteType", hike.routeType)
                    putExtra("isCompleted", hike.isCompleted)
                    putExtra("completedDate", hike.completedDate)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val binding = RecentHikeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HikeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        val hike = hikeList[position]
        holder.bind(hike)
    }

    override fun getItemCount(): Int = hikeList.size

    fun updateData(newList: List<Hike>) {
        hikeList.clear()
        hikeList.addAll(newList)
        notifyDataSetChanged()
    }
}
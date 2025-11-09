package com.example.hikermanagementapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HikeListAdapter(
    private var hikeList: MutableList<Hike>,
    private val dbHelper: HikeDbHelper,
    private val context: Context
) : RecyclerView.Adapter<HikeListAdapter.HikeViewHolder>() {

    inner class HikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hikeName: TextView = itemView.findViewById(R.id.hikeName)
        val hikeLocation: TextView = itemView.findViewById(R.id.hikeLocation)
        val hikeDifficulty: TextView = itemView.findViewById(R.id.hikeDifficulty)
        val hikeDate: TextView = itemView.findViewById(R.id.hikeDate)
        val hikeLength: TextView = itemView.findViewById(R.id.hikeLength)
        val hikeParking: TextView = itemView.findViewById(R.id.hikeParking)
        val viewDetailsIcon: ImageView = itemView.findViewById(R.id.viewDetailsIcon)
        val observationsContainer: LinearLayout = itemView.findViewById(R.id.observationsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hike_item, parent, false)
        return HikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        val hike = hikeList[position]

        // Display main info
        holder.hikeName.text = hike.name
        holder.hikeLocation.text = hike.location
        holder.hikeDate.text = hike.date
        holder.hikeLength.text = "${hike.length} km"
        holder.hikeDifficulty.text = hike.difficulty
        holder.hikeParking.text = hike.parking

        // For vertical list, show observations and details icon
        holder.observationsContainer.visibility = View.VISIBLE
        holder.viewDetailsIcon.visibility = View.VISIBLE

        // Observations section
        holder.observationsContainer.removeAllViews()
        val observations = dbHelper.getObservationsForHike(hike.id)
        if (observations.isEmpty()) {
            val tv = TextView(holder.itemView.context).apply {
                text = "No observations yet"
                textSize = 12f
                setTextColor(Color.parseColor("#666666"))
            }
            holder.observationsContainer.addView(tv)
        } else {
            for (obs in observations.take(3)) {
                val tv = TextView(holder.itemView.context).apply {
                    text = "• ${obs.observation} (${obs.obsTime})"
                    textSize = 12f
                    setTextColor(Color.parseColor("#666666"))
                    setPadding(0, 4, 0, 4)
                }
                holder.observationsContainer.addView(tv)
            }

            if (observations.size > 3) {
                val tv = TextView(holder.itemView.context).apply {
                    text = "• View ${observations.size - 3} more observations..."
                    textSize = 12f
                    setTextColor(Color.parseColor("#2196F3"))
                    setPadding(0, 4, 0, 4)
                }
                holder.observationsContainer.addView(tv)
            }
        }

        // Set difficulty background color with proper padding
        val difficultyColor = when (hike.difficulty.lowercase()) {
            "easy" -> Color.parseColor("#4CAF50")
            "moderate" -> Color.parseColor("#FFC107")
            "hard" -> Color.parseColor("#F44336")
            else -> Color.parseColor("#607D8B")
        }
        holder.hikeDifficulty.setBackgroundColor(difficultyColor)
        holder.hikeDifficulty.setPadding(16, 8, 16, 8)
        holder.hikeDifficulty.setTextColor(Color.WHITE)

        // Click listener to open details
        holder.itemView.setOnClickListener {
            openHikeDetails(hike)
        }

        holder.viewDetailsIcon.setOnClickListener {
            openHikeDetails(hike)
        }
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
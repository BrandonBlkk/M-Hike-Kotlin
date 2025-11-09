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

class HikeAdapter(
    private var hikeList: MutableList<Hike>,
    private val dbHelper: HikeDbHelper,
    private val context: Context
) : RecyclerView.Adapter<HikeAdapter.HikeViewHolder>() {

    inner class HikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hikeName: TextView = itemView.findViewById(R.id.hikeName)
        val hikeLocation: TextView = itemView.findViewById(R.id.hikeLocation)
        val hikeDifficulty: TextView = itemView.findViewById(R.id.hikeDifficulty)
        val hikeDate: TextView = itemView.findViewById(R.id.hikeDate)
        val viewDetailsIcon: ImageView = itemView.findViewById(R.id.viewDetailsIcon)
        val observationsContainer: LinearLayout = itemView.findViewById(R.id.observationsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hike, parent, false)
        return HikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        val hike = hikeList[position]

        // Display main info
        holder.hikeName.text = hike.name
        holder.hikeLocation.text = "${hike.location} • ${hike.length} km"
        holder.hikeDifficulty.text = hike.difficulty
        holder.hikeDate.text = hike.date

        // Highlight difficulty color
        when (hike.difficulty.lowercase()) {
            "easy" -> holder.hikeDifficulty.setBackgroundColor(Color.parseColor("#4CAF50")) // green
            "moderate" -> holder.hikeDifficulty.setBackgroundColor(Color.parseColor("#FFC107")) // amber
            "hard" -> holder.hikeDifficulty.setBackgroundColor(Color.parseColor("#F44336")) // red
            else -> holder.hikeDifficulty.setBackgroundColor(Color.parseColor("#607D8B")) // grey
        }

        // Observations section
        holder.observationsContainer.removeAllViews()
        val observations = dbHelper.getObservationsForHike(hike.id)
        if (observations.isEmpty()) {
            val tv = TextView(holder.itemView.context).apply {
                text = "No observations yet"
                textSize = 12f
                setTextColor(Color.parseColor("#333333"))
            }
            holder.observationsContainer.addView(tv)
        } else {
            for (obs in observations) {
                val tv = TextView(holder.itemView.context).apply {
                    text = "• ${obs.observation} (${obs.obsTime})"
                    textSize = 12f
                    setTextColor(Color.parseColor("#333333"))
                    setPadding(0, 4, 0, 4)
                }
                holder.observationsContainer.addView(tv)
            }
        }

        // Click listener to open details
        holder.viewDetailsIcon.setOnClickListener {
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
    }

    override fun getItemCount(): Int = hikeList.size

    fun updateData(newList: List<Hike>) {
        hikeList.clear()
        hikeList.addAll(newList)
        notifyDataSetChanged()
    }
}
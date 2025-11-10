package com.example.hikermanagementapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hikermanagementapplication.databinding.ItemDailyWeatherBinding

class DailyWeatherAdapter(private val dailyList: List<DailyWeather>) :
    RecyclerView.Adapter<DailyWeatherAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDailyWeatherBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dailyWeather = dailyList[position]
        holder.binding.dayText.text = dailyWeather.day
        holder.binding.maxTempText.text = dailyWeather.maxTemp
        holder.binding.minTempText.text = dailyWeather.minTemp
        holder.binding.dailyWeatherIcon.setImageResource(dailyWeather.iconRes)
    }

    override fun getItemCount(): Int = dailyList.size
}
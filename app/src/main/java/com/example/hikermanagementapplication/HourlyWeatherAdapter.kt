package com.example.hikermanagementapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hikermanagementapplication.databinding.ItemHourlyWeatherBinding

class HourlyWeatherAdapter(private val hourlyList: List<HourlyWeather>) :
    RecyclerView.Adapter<HourlyWeatherAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHourlyWeatherBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hourlyWeather = hourlyList[position]
        holder.binding.hourlyTimeText.text = hourlyWeather.time
        holder.binding.hourlyTempText.text = hourlyWeather.temperature
        holder.binding.hourlyWeatherIcon.setImageResource(hourlyWeather.iconRes)
    }

    override fun getItemCount(): Int = hourlyList.size
}
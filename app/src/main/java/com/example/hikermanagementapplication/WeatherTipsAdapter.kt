package com.example.hikermanagementapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hikermanagementapplication.databinding.ItemWeatherTipBinding

class WeatherTipsAdapter(private val tipsList: List<WeatherTip>) :
    RecyclerView.Adapter<WeatherTipsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemWeatherTipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeatherTipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tip = tipsList[position]
        holder.binding.tipText.text = tip.tip
        holder.binding.tipIcon.setImageResource(tip.iconRes)
    }

    override fun getItemCount(): Int = tipsList.size
}
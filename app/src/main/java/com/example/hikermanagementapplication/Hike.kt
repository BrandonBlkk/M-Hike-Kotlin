package com.example.hikermanagementapplication

data class Hike(
    val id: Long = 0,
    val name: String,
    val location: String,
    val date: String,
    val parking: String,
    val length: Double,
    val difficulty: String,
    val status: String,
    val description: String? = null,
    val trailType: String? = null,
    val weather: String? = null
)
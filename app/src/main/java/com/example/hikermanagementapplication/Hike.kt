package com.example.hikermanagementapplication

data class Hike(
    val id: Long = 0,
    val name: String,
    val location: String,
    val date: String,
    val parking: String,
    val length: Double,
    val routeType: String? = null,
    val difficulty: String,
    val description: String? = null,
    val notes: String? = null,
    val weather: String? = null,
    val isCompleted: Int = 0,
    val completedDate: String? = null,
    val createdAt: String? = null
)
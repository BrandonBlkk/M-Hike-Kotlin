package com.example.hikermanagementapplication

data class Observation(
    val obsId: Long = 0,
    val hikeId: Long,
    val observation: String,
    val obsTime: String,
    val comments: String? = null
)
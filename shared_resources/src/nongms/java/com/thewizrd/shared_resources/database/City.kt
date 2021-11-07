package com.thewizrd.shared_resources.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data")
data class City(
    @PrimaryKey
    @NonNull
    val id: Long,
    val name: String,
    val state: String?,
    val country: String?,
    val lat: Double,
    val lon: Double
)
package com.progetto.Mappa

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey val id: Int = 1,
    val latitude: Double,
    val longitude: Double
)

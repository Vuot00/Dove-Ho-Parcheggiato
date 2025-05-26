package com.progetto.Mappa

import androidx.room.*

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations WHERE id = 1")
    suspend fun getLocation(): SavedLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations")
    suspend fun deleteLocation()
}

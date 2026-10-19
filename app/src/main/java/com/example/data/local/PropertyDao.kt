package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {

    @Query("SELECT * FROM saved_properties ORDER BY savedTimestamp DESC")
    fun getAllSavedProperties(): Flow<List<Property>>

    @Query("SELECT * FROM saved_properties ORDER BY savedTimestamp DESC")
    fun getAllProperties(): Flow<List<Property>>

    @Query("SELECT id FROM saved_properties")
    fun getAllSavedPropertyIds(): Flow<List<String>>

    @Query("SELECT * FROM saved_properties WHERE id = :id LIMIT 1")
    suspend fun getSavedPropertyById(id: String): Property?

    @Query("SELECT * FROM saved_properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: String): Property?

    @Query("SELECT COUNT(*) FROM saved_properties")
    suspend fun getSavedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedProperty(property: Property)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSavedProperties(properties: List<Property>)

    @Update
    suspend fun updateProperty(property: Property)

    @Update
    suspend fun updateSavedProperty(property: Property)

    @Delete
    suspend fun deleteProperty(property: Property)

    @Delete
    suspend fun deleteSavedProperty(property: Property)

    @Query("DELETE FROM saved_properties WHERE id = :id")
    suspend fun deletePropertyById(id: String)

    @Query("DELETE FROM saved_properties WHERE id = :id")
    suspend fun deleteSavedPropertyById(id: String)

    @Query("DELETE FROM saved_properties")
    suspend fun deleteAllSavedProperties()

    // Price Alerts
    @Query("SELECT * FROM price_alerts ORDER BY timestamp DESC")
    fun getAllPriceAlerts(): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlertEntity)

    @Query("UPDATE price_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: String)
}

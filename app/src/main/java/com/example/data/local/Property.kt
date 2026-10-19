package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PriceAlert
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType

@Entity(tableName = "saved_properties")
data class Property(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val previousPrice: Int? = null,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceFromCampusKm: Double,
    val beds: Int,
    val baths: Double,
    val sqft: Int,
    val propertyType: PropertyType,
    val amenities: List<String>,
    val imageUrls: List<String>,
    val rating: Double,
    val reviewsCount: Int,
    val verifiedStudentHousing: Boolean,
    val landlordName: String,
    val landlordPhone: String,
    val leaseTermsMonths: Int,
    val safetyScore: Int,
    val walkScore: Int,
    val estimatedUtilitiesMonthly: Int,
    val availableDate: String,
    val virtualTourAvailable: Boolean,
    val savedTimestamp: Long = System.currentTimeMillis()
)

typealias SavedPropertyEntity = Property

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val oldPrice: Int,
    val newPrice: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

fun PropertyListing.toSavedEntity(savedTimestamp: Long = System.currentTimeMillis()): Property {
    return Property(
        id = id,
        title = title,
        description = description,
        price = price,
        previousPrice = previousPrice,
        address = address,
        latitude = latitude,
        longitude = longitude,
        distanceFromCampusKm = distanceFromCampusKm,
        beds = beds,
        baths = baths,
        sqft = sqft,
        propertyType = propertyType,
        amenities = amenities,
        imageUrls = imageUrls,
        rating = rating,
        reviewsCount = reviewsCount,
        verifiedStudentHousing = verifiedStudentHousing,
        landlordName = landlordName,
        landlordPhone = landlordPhone,
        leaseTermsMonths = leaseTermsMonths,
        safetyScore = safetyScore,
        walkScore = walkScore,
        estimatedUtilitiesMonthly = estimatedUtilitiesMonthly,
        availableDate = availableDate,
        virtualTourAvailable = virtualTourAvailable,
        savedTimestamp = savedTimestamp
    )
}

fun Property.toDomainModel(): PropertyListing {
    return PropertyListing(
        id = id,
        title = title,
        description = description,
        price = price,
        previousPrice = previousPrice,
        address = address,
        latitude = latitude,
        longitude = longitude,
        distanceFromCampusKm = distanceFromCampusKm,
        beds = beds,
        baths = baths,
        sqft = sqft,
        propertyType = propertyType,
        amenities = amenities,
        imageUrls = imageUrls,
        rating = rating,
        reviewsCount = reviewsCount,
        isSaved = true,
        verifiedStudentHousing = verifiedStudentHousing,
        landlordName = landlordName,
        landlordPhone = landlordPhone,
        leaseTermsMonths = leaseTermsMonths,
        safetyScore = safetyScore,
        walkScore = walkScore,
        estimatedUtilitiesMonthly = estimatedUtilitiesMonthly,
        availableDate = availableDate,
        virtualTourAvailable = virtualTourAvailable
    )
}

fun PriceAlert.toEntity(): PriceAlertEntity {
    return PriceAlertEntity(
        id = id,
        propertyId = propertyId,
        propertyTitle = propertyTitle,
        oldPrice = oldPrice,
        newPrice = newPrice,
        timestamp = timestamp,
        isRead = isRead
    )
}

fun PriceAlertEntity.toDomainModel(): PriceAlert {
    return PriceAlert(
        id = id,
        propertyId = propertyId,
        propertyTitle = propertyTitle,
        oldPrice = oldPrice,
        newPrice = newPrice,
        timestamp = timestamp,
        isRead = isRead
    )
}

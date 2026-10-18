package com.example.data.model

import com.example.ui.theme.ThemeMode

enum class PropertyType(val label: String) {
    STUDIO("Studio"),
    APARTMENT("Apartment"),
    SHARED_SUITE("Shared Suite"),
    CONDO("Condo"),
    TOWNHOUSE("Townhouse")
}

data class PropertyListing(
    val id: String,
    val title: String,
    val description: String,
    val price: Int, // monthly rent in USD
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
    val rating: Double = 4.8,
    val reviewsCount: Int = 24,
    val isSaved: Boolean = false,
    val verifiedStudentHousing: Boolean = true,
    val landlordName: String = "Campus Living Partners",
    val landlordPhone: String = "+1 (555) 234-5678",
    val leaseTermsMonths: Int = 12,
    val safetyScore: Int = 94, // 0-100
    val walkScore: Int = 92, // 0-100
    val estimatedUtilitiesMonthly: Int = 65,
    val availableDate: String = "Aug 1, 2026",
    val virtualTourAvailable: Boolean = true
) {
    val hasPriceDrop: Boolean
        get() = previousPrice != null && previousPrice > price

    val priceDropAmount: Int
        get() = if (hasPriceDrop) (previousPrice!! - price) else 0

    val pricePerBed: Int
        get() = if (beds > 0) price / beds else price
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val propertyContextId: String? = null,
    val suggestedFollowUps: List<String> = emptyList()
)

data class UserProfile(
    val id: String = "usr_9421",
    val name: String = "Alex Chen",
    val email: String = "alex.chen@university.edu",
    val campus: String = "Metro Tech University",
    val budgetMax: Int = 1400,
    val preferredBeds: Int = 2,
    val notifyOnPriceChanges: Boolean = true,
    val themePreference: ThemeMode = ThemeMode.SYSTEM,
    val isAuthenticated: Boolean = true
)

data class PriceAlert(
    val id: String,
    val propertyId: String,
    val propertyTitle: String,
    val oldPrice: Int,
    val newPrice: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

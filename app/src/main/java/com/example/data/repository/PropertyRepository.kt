package com.example.data.repository

import android.content.Context
import com.example.data.api.GeminiAdvisorService
import com.example.data.model.ChatMessage
import com.example.data.model.PriceAlert
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType
import com.example.data.model.UserProfile
import com.example.data.util.NotificationHelper
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PropertyFilter(
    val query: String = "",
    val maxPrice: Int = 3000,
    val minBeds: Int = 0,
    val selectedType: PropertyType? = null,
    val onlyStudentVerified: Boolean = false,
    val maxDistanceKm: Double = 10.0,
    val requiredAmenities: Set<String> = emptySet(),
    val onlyPriceDrops: Boolean = false
)

class PropertyRepository(
    private val geminiAdvisorService: GeminiAdvisorService = GeminiAdvisorService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _properties = MutableStateFlow<List<PropertyListing>>(createSampleProperties())
    val properties: StateFlow<List<PropertyListing>> = _properties.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _priceAlerts = MutableStateFlow<List<PriceAlert>>(emptyList())
    val priceAlerts: StateFlow<List<PriceAlert>> = _priceAlerts.asStateFlow()

    private val _filter = MutableStateFlow(PropertyFilter())
    val filter: StateFlow<PropertyFilter> = _filter.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(createInitialChat())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAdvisorThinking = MutableStateFlow(false)
    val isAdvisorThinking: StateFlow<Boolean> = _isAdvisorThinking.asStateFlow()

    private val _activeChatProperty = MutableStateFlow<PropertyListing?>(null)
    val activeChatProperty: StateFlow<PropertyListing?> = _activeChatProperty.asStateFlow()

    fun updateFilter(update: (PropertyFilter) -> PropertyFilter) {
        _filter.value = update(_filter.value)
    }

    fun resetFilter() {
        _filter.value = PropertyFilter()
    }

    fun toggleSave(propertyId: String, context: Context? = null) {
        val currentList = _properties.value
        val updated = currentList.map { prop ->
            if (prop.id == propertyId) {
                val newSavedState = !prop.isSaved
                prop.copy(isSaved = newSavedState)
            } else {
                prop
            }
        }
        _properties.value = updated
    }

    fun setActiveChatProperty(property: PropertyListing?) {
        _activeChatProperty.value = property
        if (property != null) {
            val systemContextMsg = ChatMessage(
                text = "📍 Switched context to **${property.title}** ($${property.price}/mo • ${property.beds} Bed • ${property.distanceFromCampusKm} km from campus). What would you like to know about this listing?",
                isUser = false,
                propertyContextId = property.id,
                suggestedFollowUps = listOf(
                    "Are utilities included?",
                    "What are the amenities?",
                    "Calculate rent split for roommates",
                    "How far is the walk to campus?"
                )
            )
            _chatMessages.value = _chatMessages.value + systemContextMsg
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val currentProp = _activeChatProperty.value
        val userMsg = ChatMessage(
            text = userText,
            isUser = true,
            propertyContextId = currentProp?.id
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isAdvisorThinking.value = true

        scope.launch {
            val history = _chatMessages.value.takeLast(6).map { it.text to it.isUser }
            val responseText = geminiAdvisorService.askAdvisor(
                userMessage = userText,
                selectedProperty = currentProp,
                allProperties = _properties.value,
                conversationHistory = history
            )

            val suggested = if (currentProp != null) {
                listOf("Check lease terms", "Show nearby alternatives", "Ask about pet policy")
            } else {
                listOf("Show listings under $1,000", "Which is closest to campus?", "Tips for signing a lease")
            }

            val aiMsg = ChatMessage(
                text = responseText,
                isUser = false,
                propertyContextId = currentProp?.id,
                suggestedFollowUps = suggested
            )

            _chatMessages.value = _chatMessages.value + aiMsg
            _isAdvisorThinking.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = createInitialChat()
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    fun updateThemePreference(mode: ThemeMode) {
        _userProfile.value = _userProfile.value.copy(themePreference = mode)
    }

    /**
     * Simulates real-time price changes on housing listings and pushes notifications!
     */
    fun simulateMarketPriceDrop(context: Context) {
        val list = _properties.value
        // Prefer a saved property if exists, else first available
        val target = list.firstOrNull { it.isSaved } ?: list.firstOrNull() ?: return
        val discount = listOf(50, 75, 100, 150).random()
        val oldPrice = target.price
        val newPrice = (oldPrice - discount).coerceAtLeast(400)

        val updatedList = list.map {
            if (it.id == target.id) {
                it.copy(price = newPrice, previousPrice = oldPrice)
            } else {
                it
            }
        }
        _properties.value = updatedList

        val alert = PriceAlert(
            id = UUID.randomUUID().toString(),
            propertyId = target.id,
            propertyTitle = target.title,
            oldPrice = oldPrice,
            newPrice = newPrice
        )
        _priceAlerts.value = listOf(alert) + _priceAlerts.value

        // Trigger real push notification if enabled
        if (_userProfile.value.notifyOnPriceChanges) {
            NotificationHelper.showPriceDropNotification(
                context = context,
                propertyTitle = target.title,
                oldPrice = oldPrice,
                newPrice = newPrice,
                dropAmount = discount
            )
        }
    }

    fun markAlertAsRead(alertId: String) {
        _priceAlerts.value = _priceAlerts.value.map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
    }

    private fun createInitialChat(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                text = "👋 Hi! I'm your **Smart Property Advisor**.\n\nI can help you evaluate housing listings, check walking distances to campus, compare amenities, calculate roommate splits, and spot price drops!\n\nTap a property card or choose a prompt below to get started.",
                isUser = false,
                suggestedFollowUps = listOf(
                    "Find affordable student apartments",
                    "Which properties have free in-unit laundry?",
                    "Closest listings to the Science Quad",
                    "How to avoid lease hidden fees?"
                )
            )
        )
    }

    companion object {
        fun createSampleProperties(): List<PropertyListing> = listOf(
            PropertyListing(
                id = "prop_1",
                title = "The University Lofts & Suites",
                description = "Modern student-tailored community just steps from campus. Features private study pods, high-speed fiber internet, rooftop skyline terrace, and package lockers.",
                price = 850,
                previousPrice = 950, // Has a $100 price drop!
                address = "410 Campus Drive, University District",
                latitude = 37.7749,
                longitude = -122.4194,
                distanceFromCampusKm = 0.4,
                beds = 2,
                baths = 1.0,
                sqft = 750,
                propertyType = PropertyType.APARTMENT,
                amenities = listOf("High-Speed WiFi", "In-Unit Laundry", "Furnished", "Study Lounge", "Bicycle Storage", "24/7 Security", "Utilities Included"),
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1000&auto=format&fit=crop&q=80"
                ),
                rating = 4.9,
                reviewsCount = 42,
                isSaved = true,
                verifiedStudentHousing = true,
                landlordName = "Apex Student Housing",
                landlordPhone = "+1 (555) 782-9011",
                leaseTermsMonths = 12,
                safetyScore = 96,
                walkScore = 98,
                estimatedUtilitiesMonthly = 0, // Included!
                availableDate = "Aug 15, 2026"
            ),
            PropertyListing(
                id = "prop_2",
                title = "Skyline Heights Luxury Studio",
                description = "Chic minimalist studio with floor-to-ceiling windows, stainless steel appliances, smart thermostats, and resort-style infinity pool.",
                price = 1250,
                previousPrice = 1350,
                address = "820 Metro Boulevard, Downtown Hub",
                latitude = 37.7795,
                longitude = -122.4150,
                distanceFromCampusKm = 1.8,
                beds = 1,
                baths = 1.0,
                sqft = 560,
                propertyType = PropertyType.STUDIO,
                amenities = listOf("High-Speed WiFi", "Fitness Center", "Swimming Pool", "Covered Parking", "Pet Friendly", "Rooftop Deck", "Smart Thermostat"),
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1000&auto=format&fit=crop&q=80"
                ),
                rating = 4.7,
                reviewsCount = 31,
                isSaved = false,
                verifiedStudentHousing = false,
                landlordName = "Urban Metro Realty",
                landlordPhone = "+1 (555) 349-2200",
                leaseTermsMonths = 12,
                safetyScore = 91,
                walkScore = 94,
                estimatedUtilitiesMonthly = 85,
                availableDate = "Jul 1, 2026"
            ),
            PropertyListing(
                id = "prop_3",
                title = "College Green 3-Bed Townhome",
                description = "Spacious three-bedroom townhome ideal for groups of students. Features a private enclosed patio, attached 2-car garage, and newly renovated kitchen.",
                price = 1950, // $650 per student!
                previousPrice = null,
                address = "124 Elmwood Avenue, College Park",
                latitude = 37.7710,
                longitude = -122.4280,
                distanceFromCampusKm = 0.9,
                beds = 3,
                baths = 2.0,
                sqft = 1320,
                propertyType = PropertyType.TOWNHOUSE,
                amenities = listOf("In-Unit Laundry", "Covered Parking", "Private Patio", "Pet Friendly", "Dishwasher", "Air Conditioning"),
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1000&auto=format&fit=crop&q=80"
                ),
                rating = 4.8,
                reviewsCount = 19,
                isSaved = true,
                verifiedStudentHousing = true,
                landlordName = "Greenside Properties",
                landlordPhone = "+1 (555) 601-4433",
                leaseTermsMonths = 12,
                safetyScore = 95,
                walkScore = 89,
                estimatedUtilitiesMonthly = 130,
                availableDate = "Aug 1, 2026"
            ),
            PropertyListing(
                id = "prop_4",
                title = "The Hub Innovation Shared Suite",
                description = "All-inclusive co-living community for engineering and tech students. Fully furnished room with en-suite private bath, co-working desks, and weekly cleanings.",
                price = 720,
                previousPrice = 780,
                address = "550 Tech Corridor, Silicon Way",
                latitude = 37.7820,
                longitude = -122.4080,
                distanceFromCampusKm = 2.2,
                beds = 1,
                baths = 1.0,
                sqft = 320,
                propertyType = PropertyType.SHARED_SUITE,
                amenities = listOf("High-Speed WiFi", "Furnished", "Utilities Included", "Study Lounge", "Fitness Center", "Weekly Housekeeping", "Bicycle Storage"),
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?w=1000&auto=format&fit=crop&q=80"
                ),
                rating = 4.6,
                reviewsCount = 58,
                isSaved = false,
                verifiedStudentHousing = true,
                landlordName = "The Hub Co-Living",
                landlordPhone = "+1 (555) 880-1234",
                leaseTermsMonths = 9,
                safetyScore = 93,
                walkScore = 85,
                estimatedUtilitiesMonthly = 0,
                availableDate = "Immediate"
            ),
            PropertyListing(
                id = "prop_5",
                title = "Parkside Garden 2B2B Condo",
                description = "Quiet and tranquil condo overlooking the botanical park. Sunny corner unit with hardwood floors, modern quartz countertops, and fast transit link.",
                price = 1450,
                previousPrice = null,
                address = "315 Willow Way, South Gate",
                latitude = 37.7680,
                longitude = -122.4220,
                distanceFromCampusKm = 1.4,
                beds = 2,
                baths = 2.0,
                sqft = 920,
                propertyType = PropertyType.CONDO,
                amenities = listOf("High-Speed WiFi", "In-Unit Laundry", "Balcony", "Covered Parking", "Fitness Center", "Dishwasher", "Pet Friendly"),
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1484154218962-a197022b5858?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1560185007-c5ca9d2c014d?w=1000&auto=format&fit=crop&q=80"
                ),
                rating = 4.9,
                reviewsCount = 14,
                isSaved = false,
                verifiedStudentHousing = true,
                landlordName = "Parkside Realty Group",
                landlordPhone = "+1 (555) 432-8765",
                leaseTermsMonths = 12,
                safetyScore = 97,
                walkScore = 91,
                estimatedUtilitiesMonthly = 70,
                availableDate = "Sep 1, 2026"
            ),
            PropertyListing(
                id = "prop_6",
                title = "Varsity Point Academic Studios",
                description = "Directly adjacent to the Main Library and Science Complex. Ultra-convenient location with sound-insulated walls and high-speed gigabit Ethernet.",
                price = 920,
                previousPrice = 990,
                address = "12 Library Plaza, Campus North",
                latitude = 37.7760,
                longitude = -122.4210,
                distanceFromCampusKm = 0.2,
                beds = 1,
                baths = 1.0,
                sqft = 450,
                propertyType = PropertyType.STUDIO,
                amenities = listOf("High-Speed WiFi", "Furnished", "Study Lounge", "24/7 Security", "Bicycle Storage", "Utilities Included"),
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1536376072261-38c75010e6c9?w=1000&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1000&auto=format&fit=crop&q=80"
                ),
                rating = 5.0,
                reviewsCount = 67,
                isSaved = true,
                verifiedStudentHousing = true,
                landlordName = "Varsity Housing Co",
                landlordPhone = "+1 (555) 910-8822",
                leaseTermsMonths = 12,
                safetyScore = 99,
                walkScore = 99,
                estimatedUtilitiesMonthly = 0,
                availableDate = "Aug 20, 2026"
            )
        )
    }
}

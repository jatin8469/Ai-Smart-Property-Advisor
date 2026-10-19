package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.PriceAlert
import com.example.data.model.PropertyListing
import com.example.data.model.PropertyType
import com.example.data.model.UserProfile
import com.example.data.repository.PropertyFilter
import com.example.data.repository.PropertyRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ScreenDestination {
    EXPLORE,
    MAP,
    ADVISOR,
    SAVED,
    PROFILE
}

class MainViewModel(
    val repository: PropertyRepository = PropertyRepository()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(ScreenDestination.EXPLORE)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _selectedPropertyDetail = MutableStateFlow<PropertyListing?>(null)
    val selectedPropertyDetail: StateFlow<PropertyListing?> = _selectedPropertyDetail.asStateFlow()

    private val _showFilterSheet = MutableStateFlow(false)
    val showFilterSheet: StateFlow<Boolean> = _showFilterSheet.asStateFlow()

    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    // State flows from repository
    val allProperties: StateFlow<List<PropertyListing>> = repository.properties
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val priceAlerts: StateFlow<List<PriceAlert>> = repository.priceAlerts
    val filter: StateFlow<PropertyFilter> = repository.filter
    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
    val isAdvisorThinking: StateFlow<Boolean> = repository.isAdvisorThinking
    val activeChatProperty: StateFlow<PropertyListing?> = repository.activeChatProperty

    // Filtered listings
    val filteredProperties: StateFlow<List<PropertyListing>> =
        combine(allProperties, filter) { list, f ->
            list.filter { prop ->
                val matchesQuery = f.query.isBlank() ||
                        prop.title.contains(f.query, ignoreCase = true) ||
                        prop.address.contains(f.query, ignoreCase = true) ||
                        prop.amenities.any { it.contains(f.query, ignoreCase = true) }

                val matchesPrice = prop.price <= f.maxPrice
                val matchesBeds = f.minBeds == 0 || prop.beds >= f.minBeds
                val matchesType = f.selectedType == null || prop.propertyType == f.selectedType
                val matchesVerified = !f.onlyStudentVerified || prop.verifiedStudentHousing
                val matchesDistance = prop.distanceFromCampusKm <= f.maxDistanceKm
                val matchesAmenities = f.requiredAmenities.isEmpty() ||
                        f.requiredAmenities.all { req -> prop.amenities.any { it.equals(req, ignoreCase = true) } }
                val matchesPriceDrop = !f.onlyPriceDrops || prop.hasPriceDrop

                matchesQuery && matchesPrice && matchesBeds && matchesType &&
                        matchesVerified && matchesDistance && matchesAmenities && matchesPriceDrop
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedProperties: StateFlow<List<PropertyListing>> =
        repository.savedPropertiesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun openPropertyDetail(property: PropertyListing) {
        _selectedPropertyDetail.value = property
    }

    fun closePropertyDetail() {
        _selectedPropertyDetail.value = null
    }

    fun openAdvisorForProperty(property: PropertyListing) {
        repository.setActiveChatProperty(property)
        _selectedPropertyDetail.value = null
        _currentScreen.value = ScreenDestination.ADVISOR
    }

    fun toggleFilterSheet(show: Boolean) {
        _showFilterSheet.value = show
    }

    fun toggleAuthDialog(show: Boolean) {
        _showAuthDialog.value = show
    }

    fun setQuery(query: String) {
        repository.updateFilter { it.copy(query = query) }
    }

    fun setMaxPrice(max: Int) {
        repository.updateFilter { it.copy(maxPrice = max) }
    }

    fun setMinBeds(beds: Int) {
        repository.updateFilter { it.copy(minBeds = beds) }
    }

    fun setPropertyType(type: PropertyType?) {
        repository.updateFilter { it.copy(selectedType = type) }
    }

    fun toggleAmenityFilter(amenity: String) {
        repository.updateFilter { current ->
            val set = current.requiredAmenities.toMutableSet()
            if (set.contains(amenity)) set.remove(amenity) else set.add(amenity)
            current.copy(requiredAmenities = set)
        }
    }

    fun toggleOnlyPriceDrops(enabled: Boolean) {
        repository.updateFilter { it.copy(onlyPriceDrops = enabled) }
    }

    fun resetFilters() {
        repository.resetFilter()
    }

    fun toggleSave(propertyId: String, context: Context) {
        repository.toggleSave(propertyId, context)
        val currentDetail = _selectedPropertyDetail.value
        if (currentDetail != null && currentDetail.id == propertyId) {
            _selectedPropertyDetail.value = currentDetail.copy(isSaved = !currentDetail.isSaved)
        }
    }

    fun simulatePriceDrop(context: Context) {
        repository.simulateMarketPriceDrop(context)
    }

    fun sendChatMessage(text: String) {
        repository.sendChatMessage(text)
    }

    fun clearChat() {
        repository.clearChat()
    }

    fun clearChatContext() {
        repository.setActiveChatProperty(null)
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.updateThemePreference(mode)
    }

    fun toggleNotifications(enabled: Boolean) {
        repository.updateUserProfile(userProfile.value.copy(notifyOnPriceChanges = enabled))
    }

    fun signIn(email: String, name: String) {
        repository.updateUserProfile(
            userProfile.value.copy(
                email = email,
                name = name,
                isAuthenticated = true
            )
        )
        _showAuthDialog.value = false
    }

    fun signOut() {
        repository.updateUserProfile(
            userProfile.value.copy(
                isAuthenticated = false
            )
        )
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.getInstance(appContext).propertyDao()
                    val repo = PropertyRepository(propertyDao = dao)
                    return MainViewModel(repository = repo) as T
                }
            }
        }
    }
}

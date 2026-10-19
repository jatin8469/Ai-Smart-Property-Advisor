package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.util.NotificationHelper
import com.example.ui.components.AppTopBar
import com.example.ui.components.FilterBottomSheet
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.SavedListingsScreen
import com.example.ui.theme.SmartPropertyTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.initChannel(this)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(this@MainActivity)
            )
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

            SmartPropertyTheme(themeMode = userProfile.themePreference) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedPropertyDetail.collectAsStateWithLifecycle()
    val showFilterSheet by viewModel.showFilterSheet.collectAsStateWithLifecycle()

    val filteredProperties by viewModel.filteredProperties.collectAsStateWithLifecycle()
    val allProperties by viewModel.allProperties.collectAsStateWithLifecycle()
    val savedProperties by viewModel.savedProperties.collectAsStateWithLifecycle()
    val priceAlerts by viewModel.priceAlerts.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAdvisorThinking by viewModel.isAdvisorThinking.collectAsStateWithLifecycle()
    val activeChatProperty by viewModel.activeChatProperty.collectAsStateWithLifecycle()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle back button for sub-screens / details
    BackHandler(enabled = selectedDetail != null || currentScreen != ScreenDestination.EXPLORE) {
        if (selectedDetail != null) {
            viewModel.closePropertyDetail()
        } else {
            viewModel.navigateTo(ScreenDestination.EXPLORE)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (selectedDetail == null) {
                AppTopBar(
                    userProfile = userProfile,
                    priceAlerts = priceAlerts,
                    themeMode = userProfile.themePreference,
                    onToggleTheme = { viewModel.setThemeMode(it) },
                    onOpenFilter = { viewModel.toggleFilterSheet(true) },
                    onOpenProfile = { viewModel.navigateTo(ScreenDestination.PROFILE) },
                    onSimulatePriceDrop = { viewModel.simulatePriceDrop(context) }
                )
            }
        },
        bottomBar = {
            if (selectedDetail == null) {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.EXPLORE,
                        onClick = { viewModel.navigateTo(ScreenDestination.EXPLORE) },
                        icon = { Icon(imageVector = Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("nav_explore")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.MAP,
                        onClick = { viewModel.navigateTo(ScreenDestination.MAP) },
                        icon = { Icon(imageVector = Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") },
                        modifier = Modifier.testTag("nav_map")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.ADVISOR,
                        onClick = { viewModel.navigateTo(ScreenDestination.ADVISOR) },
                        icon = {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Advisor")
                        },
                        label = { Text("AI Advisor") },
                        modifier = Modifier.testTag("nav_advisor")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.SAVED,
                        onClick = { viewModel.navigateTo(ScreenDestination.SAVED) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (savedProperties.isNotEmpty()) {
                                        Badge { Text(savedProperties.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Saved")
                            }
                        },
                        label = { Text("Saved") },
                        modifier = Modifier.testTag("nav_saved")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.PROFILE,
                        onClick = { viewModel.navigateTo(ScreenDestination.PROFILE) },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedDetail != null) {
                PropertyDetailScreen(
                    property = selectedDetail!!,
                    onBack = { viewModel.closePropertyDetail() },
                    onSaveToggle = { viewModel.toggleSave(selectedDetail!!.id, context) },
                    onAskAi = { viewModel.openAdvisorForProperty(selectedDetail!!) }
                )
            } else {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screenTransition"
                ) { screen ->
                    when (screen) {
                        ScreenDestination.EXPLORE -> {
                            HomeScreen(
                                properties = filteredProperties,
                                filter = filter,
                                onQueryChange = { viewModel.setQuery(it) },
                                onOpenFilterSheet = { viewModel.toggleFilterSheet(true) },
                                onPropertyClick = { viewModel.openPropertyDetail(it) },
                                onSaveToggle = { viewModel.toggleSave(it.id, context) },
                                onAskAi = { viewModel.openAdvisorForProperty(it) },
                                onTogglePriceDrops = { viewModel.toggleOnlyPriceDrops(it) },
                                onToggleAmenity = { viewModel.toggleAmenityFilter(it) },
                                onSimulatePriceDrop = { viewModel.simulatePriceDrop(context) }
                            )
                        }

                        ScreenDestination.MAP -> {
                            MapScreen(
                                properties = filteredProperties,
                                onPropertyClick = { viewModel.openPropertyDetail(it) },
                                onAskAi = { viewModel.openAdvisorForProperty(it) }
                            )
                        }

                        ScreenDestination.ADVISOR -> {
                            AiChatScreen(
                                messages = chatMessages,
                                activeProperty = activeChatProperty,
                                allProperties = allProperties,
                                isThinking = isAdvisorThinking,
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onClearChat = { viewModel.clearChat() },
                                onSelectPropertyContext = { viewModel.openAdvisorForProperty(it ?: return@AiChatScreen viewModel.clearChatContext()) }
                            )
                        }

                        ScreenDestination.SAVED -> {
                            SavedListingsScreen(
                                savedProperties = savedProperties,
                                priceAlerts = priceAlerts,
                                onPropertyClick = { viewModel.openPropertyDetail(it) },
                                onRemoveSaved = { viewModel.toggleSave(it.id, context) },
                                onAskAi = { viewModel.openAdvisorForProperty(it) },
                                onSimulatePriceDrop = { viewModel.simulatePriceDrop(context) },
                                onExploreClick = { viewModel.navigateTo(ScreenDestination.EXPLORE) }
                            )
                        }

                        ScreenDestination.PROFILE -> {
                            ProfileScreen(
                                userProfile = userProfile,
                                themeMode = userProfile.themePreference,
                                onThemeChange = { viewModel.setThemeMode(it) },
                                onToggleNotifications = { viewModel.toggleNotifications(it) },
                                onSignIn = { email, name -> viewModel.signIn(email, name) },
                                onSignOut = { viewModel.signOut() },
                                onUpdateProfile = { viewModel.repository.updateUserProfile(it) },
                                onSimulateNotification = { viewModel.simulatePriceDrop(context) }
                            )
                        }
                    }
                }
            }

            // Filter Bottom Sheet
            if (showFilterSheet) {
                FilterBottomSheet(
                    filter = filter,
                    onMaxPriceChange = { viewModel.setMaxPrice(it) },
                    onMinBedsChange = { viewModel.setMinBeds(it) },
                    onTypeChange = { viewModel.setPropertyType(it) },
                    onAmenityToggle = { viewModel.toggleAmenityFilter(it) },
                    onPriceDropToggle = { viewModel.toggleOnlyPriceDrops(it) },
                    onReset = { viewModel.resetFilters() },
                    onDismiss = { viewModel.toggleFilterSheet(false) }
                )
            }
        }
    }
}

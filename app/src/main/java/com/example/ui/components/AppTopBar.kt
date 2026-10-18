package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceAlert
import com.example.data.model.UserProfile
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    userProfile: UserProfile,
    priceAlerts: List<PriceAlert>,
    themeMode: ThemeMode,
    onToggleTheme: (ThemeMode) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenProfile: () -> Unit,
    onSimulatePriceDrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showAlertsMenu by remember { mutableStateOf(false) }
    val unreadAlerts = priceAlerts.count { !it.isRead }

    TopAppBar(
        modifier = modifier.testTag("app_top_bar"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Advisor Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Smart Property",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = userProfile.campus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        actions = {
            // Price Change Notification Bell
            Box {
                IconButton(
                    onClick = { showAlertsMenu = true },
                    modifier = Modifier.testTag("alerts_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (priceAlerts.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(text = priceAlerts.size.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (unreadAlerts > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Price Drop Alerts",
                            tint = if (unreadAlerts > 0) AccentAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                DropdownMenu(
                    expanded = showAlertsMenu,
                    onDismissRequest = { showAlertsMenu = false }
                ) {
                    Text(
                        text = "Real-Time Price Alerts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    if (priceAlerts.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "No price drops yet. Tap below to simulate a market price change!",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            onClick = {
                                showAlertsMenu = false
                                onSimulatePriceDrop()
                            }
                        )
                    } else {
                        priceAlerts.take(4).forEach { alert ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = alert.propertyTitle,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Dropped from $${alert.oldPrice} to $${alert.newPrice}/mo!",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = { showAlertsMenu = false }
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "⚡ Simulate Market Price Drop",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            showAlertsMenu = false
                            onSimulatePriceDrop()
                        }
                    )
                }
            }

            // Dark Mode Quick Toggle
            Box {
                IconButton(
                    onClick = { showThemeMenu = true },
                    modifier = Modifier.testTag("dark_mode_toggle_button")
                ) {
                    Icon(
                        imageVector = when (themeMode) {
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.SYSTEM -> Icons.Default.Tune
                        },
                        contentDescription = "Theme Toggle"
                    )
                }

                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System Default") },
                        onClick = {
                            onToggleTheme(ThemeMode.SYSTEM)
                            showThemeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light Mode") },
                        onClick = {
                            onToggleTheme(ThemeMode.LIGHT)
                            showThemeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark Mode") },
                        onClick = {
                            onToggleTheme(ThemeMode.DARK)
                            showThemeMenu = false
                        }
                    )
                }
            }

            // Profile Avatar / Sign In button
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.testTag("profile_top_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

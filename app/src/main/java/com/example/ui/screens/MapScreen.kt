package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PropertyListing
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    properties: List<PropertyListing>,
    onPropertyClick: (PropertyListing) -> Unit,
    onAskAi: (PropertyListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProperty by remember { mutableStateOf<PropertyListing?>(properties.firstOrNull()) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Campus reference center
    val campusLat = 37.7750
    val campusLng = -122.4180

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_screen")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val centerX = widthPx / 2f
        val centerY = heightPx / 2f

        // Map Canvas with roads, campus quad, radius circle, and landmarks
        val isDark = MaterialTheme.colorScheme.background.red < 0.2f
        val mapBgColor = if (isDark) Color(0xFF131B2A) else Color(0xFFE2E8F0)
        val roadColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
        val parkColor = if (isDark) Color(0xFF0F291E) else Color(0xFFDCFCE7)
        val campusZoneColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.3f else 0.45f)
        val primaryThemeColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.6f, 3.0f)
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background
                drawRect(color = mapBgColor)

                // Scale & Pan origin
                val originX = centerX + panOffsetX
                val originY = centerY + panOffsetY

                // Parks / Green Zones
                drawCircle(
                    color = parkColor,
                    radius = 160f * zoomScale,
                    center = Offset(originX - 180f * zoomScale, originY + 140f * zoomScale)
                )

                // Grid of Streets / Avenues
                for (i in -4..4) {
                    val roadY = originY + i * 110f * zoomScale
                    drawLine(
                        color = roadColor,
                        start = Offset(0f, roadY),
                        end = Offset(widthPx, roadY),
                        strokeWidth = 14f * zoomScale
                    )
                }
                for (i in -4..4) {
                    val roadX = originX + i * 110f * zoomScale
                    drawLine(
                        color = roadColor,
                        start = Offset(roadX, 0f),
                        end = Offset(roadX, heightPx),
                        strokeWidth = 14f * zoomScale
                    )
                }

                // Campus Walking Radius Circle (1 km)
                drawCircle(
                    color = primaryThemeColor.copy(alpha = 0.15f),
                    radius = 240f * zoomScale,
                    center = Offset(originX, originY)
                )
                drawCircle(
                    color = primaryThemeColor.copy(alpha = 0.5f),
                    radius = 240f * zoomScale,
                    center = Offset(originX, originY),
                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f)))
                )

                // Campus Quad Zone
                drawRoundRect(
                    color = campusZoneColor,
                    topLeft = Offset(originX - 70f * zoomScale, originY - 70f * zoomScale),
                    size = androidx.compose.ui.geometry.Size(140f * zoomScale, 140f * zoomScale),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f * zoomScale)
                )
            }

            // Campus Landmark Marker (Placed at Origin)
            val campusScreenX = centerX + panOffsetX
            val campusScreenY = centerY + panOffsetY

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (campusScreenX - 70).roundToInt(),
                            (campusScreenY - 50).roundToInt()
                        )
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Campus",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Metro Tech Quad",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Property Pins
            properties.forEach { prop ->
                // Project lat/lng offset to pixel coordinate
                val dLat = (prop.latitude - campusLat) * 16000f
                val dLng = (prop.longitude - campusLng) * 16000f

                val pinX = campusScreenX + (dLng * zoomScale)
                val pinY = campusScreenY - (dLat * zoomScale)

                val isSelected = selectedProperty?.id == prop.id

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pinX - 50).roundToInt(),
                                (pinY - 45).roundToInt()
                            )
                        }
                        .clickable { selectedProperty = prop }
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("map_pin_${prop.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (prop.hasPriceDrop) {
                            Text(
                                text = "🔥",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        Text(
                            text = "$${prop.price}",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Map Control Floating Buttons (Zoom In, Zoom Out, Recenter)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(3.0f) },
                    modifier = Modifier.testTag("map_zoom_in")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f) },
                    modifier = Modifier.testTag("map_zoom_out")
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        zoomScale = 1f
                        panOffsetX = 0f
                        panOffsetY = 0f
                    },
                    modifier = Modifier.testTag("map_recenter")
                ) {
                    Icon(imageVector = Icons.Default.LocationSearching, contentDescription = "Recenter on Campus")
                }
            }
        }

        // Proximity Guide Tag (Top Left)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dotted line: 1 km Campus Walking Radius",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Selected Property Preview Card
        AnimatedVisibility(
            visible = selectedProperty != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedProperty?.let { prop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thumbnail
                            AsyncImage(
                                model = prop.imageUrls.firstOrNull(),
                                contentDescription = prop.title,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // Property Info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$${prop.price}/mo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { selectedProperty = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close preview",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = prop.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Text(
                                    text = "${prop.beds} Bed • ${prop.baths} Bath • ${prop.distanceFromCampusKm} km from campus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: View Details & Ask AI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPropertyClick(prop) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("View Details")
                            }

                            Button(
                                onClick = { onAskAi(prop) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask AI")
                            }
                        }
                    }
                }
            }
        }
    }
}

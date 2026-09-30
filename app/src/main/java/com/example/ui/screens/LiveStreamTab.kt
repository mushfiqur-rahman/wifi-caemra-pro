package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Resolution
import com.example.model.StreamConfig
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafeGreen
import com.example.util.BatteryState
import com.example.util.QrCodeGenerator

@Composable
fun LiveStreamTab(
    streamConfig: StreamConfig,
    isServerRunning: Boolean,
    localIp: String,
    batteryState: BatteryState,
    onToggleServer: () -> Unit,
    onSetAudioEnabled: (Boolean) -> Unit,
    onSetResolution: (Resolution) -> Unit,
    onSetQuality: (Int) -> Unit,
    onToggleDim: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val serverUrl = "http://$localIp:${streamConfig.port}"

    var showQrDialog by remember { mutableStateOf(false) }
    var isQrExpandedInline by remember { mutableStateOf(true) }

    val qrBitmap = remember(serverUrl) {
        QrCodeGenerator.generateQrBitmap(serverUrl, 512, 512)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-Time Low Battery Warning Alert Banner
        AnimatedVisibility(
            visible = batteryState.isLowPower,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (batteryState.isCriticalPower) AlertRed.copy(alpha = 0.2f) else AlertAmber.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (batteryState.isCriticalPower) AlertRed else AlertAmber
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("low_battery_warning_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    (if (batteryState.isCriticalPower) AlertRed else AlertAmber).copy(alpha = 0.25f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (batteryState.isCriticalPower) AlertRed else AlertAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (batteryState.isCriticalPower) "CRITICAL BATTERY: ${batteryState.level}%" else "LOW BATTERY WARNING: ${batteryState.level}%",
                                fontWeight = FontWeight.Bold,
                                color = if (batteryState.isCriticalPower) AlertRed else AlertAmber,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Device is running low on power during live streaming. Connect to a charger to prevent unexpected stream shutdown.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onToggleDim,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("low_battery_dim_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dim Screen (Save Power)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Real-Time Battery Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("battery_status_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val batteryIcon = when {
                            batteryState.isCharging -> Icons.Default.BatteryChargingFull
                            batteryState.level > 80 -> Icons.Default.BatteryFull
                            batteryState.level > 50 -> Icons.Default.Battery5Bar
                            batteryState.level > 20 -> Icons.Default.Battery3Bar
                            else -> Icons.Default.Battery0Bar
                        }

                        val batteryColor = when {
                            batteryState.isCharging -> SafeGreen
                            batteryState.level <= 20 -> AlertRed
                            batteryState.level <= 40 -> AlertAmber
                            else -> CyberTeal
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(batteryColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = batteryIcon,
                                contentDescription = "Battery Status",
                                tint = batteryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Device Power: ${batteryState.level}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (batteryState.isCharging) {
                                    Text(
                                        text = "⚡ CHARGING (${batteryState.chargePlug})",
                                        color = SafeGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (batteryState.isCharging) "Power supplied, safe for extended streaming" else "Running on internal battery",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Battery Temperature Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = if (batteryState.temperatureCelsius > 42f) AlertRed else CyberTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${"%.1f".format(batteryState.temperatureCelsius)}°C",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (batteryState.temperatureCelsius > 42f) AlertRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Battery Progress Indicator
                val progressFraction = (batteryState.level / 100f).coerceIn(0f, 1f)
                val indicatorColor = when {
                    batteryState.isCharging -> SafeGreen
                    batteryState.level <= 20 -> AlertRed
                    batteryState.level <= 40 -> AlertAmber
                    else -> CyberTeal
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = indicatorColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        // Wi-Fi Connection & QR Pairing Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wifi_connection_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyberTeal.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Wi-Fi Streaming Endpoint",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Instant pairing for desktop & laptop",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // QR toggle button
                    IconButton(
                        onClick = { isQrExpandedInline = !isQrExpandedInline },
                        modifier = Modifier.testTag("toggle_qr_inline_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Toggle QR Code",
                            tint = CyberTeal
                        )
                    }
                }

                // URL Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serverUrl,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberTeal,
                            fontSize = 15.sp
                        )

                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("WiFiCam URL", serverUrl))
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("copy_url_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "WiFiCam Live Stream URL")
                                        putExtra(Intent.EXTRA_TEXT, "Watch WiFiCam live feed on desktop/laptop: $serverUrl")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Webcam URL"))
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("share_url_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Instant Pairing QR Code Section
                AnimatedVisibility(
                    visible = isQrExpandedInline,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Scan with Laptop Webcam or Mobile to Connect",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                                    .clickable { showQrDialog = true }
                                    .testTag("qr_code_image"),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Connection Link QR Code",
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                )
                            }
                            Text(
                                text = "Tap QR code to expand fullscreen for easy desktop scanning",
                                fontSize = 11.sp,
                                color = CyberTeal
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen QR Code Dialog
        if (showQrDialog && qrBitmap != null) {
            Dialog(onDismissRequest = { showQrDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pair with Laptop / Desktop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { showQrDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Full QR Code",
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                        }

                        Text(
                            text = serverUrl,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            fontSize = 15.sp
                        )

                        Text(
                            text = "Point your laptop webcam or phone camera at this QR code to connect instantly without typing.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = { showQrDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }

        // Main Server Start / Stop Button
        Button(
            onClick = onToggleServer,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_stop_server_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServerRunning) AlertRed else ElectricBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isServerRunning) "STOP WEBCAM SERVER" else "START WI-FI WEBCAM SERVER",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }

        // Audio Stream Toggle Card (Sound with Video OR Video Only)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (streamConfig.audioEnabled) SafeGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (streamConfig.audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = if (streamConfig.audioEnabled) SafeGreen else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (streamConfig.audioEnabled) "Sound with Video" else "Video Only (Muted)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (streamConfig.audioEnabled)
                                "Live 16kHz PCM audio broadcasted to desktop"
                            else
                                "Microphone disabled for maximum privacy",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = streamConfig.audioEnabled,
                    onCheckedChange = { onSetAudioEnabled(it) },
                    modifier = Modifier.testTag("audio_record_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SafeGreen,
                        checkedTrackColor = SafeGreen.copy(alpha = 0.35f)
                    )
                )
            }
        }

        // Resolution Selector Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stream Resolution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${streamConfig.resolution.width}x${streamConfig.resolution.height}",
                        color = CyberTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Resolution.entries.forEach { res ->
                        val selected = streamConfig.resolution == res
                        FilterChip(
                            selected = selected,
                            onClick = { onSetResolution(res) },
                            label = {
                                Text(
                                    text = res.name.substringBefore("_") + " " + res.label.substringBefore(" "),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberTeal.copy(alpha = 0.25f),
                                selectedLabelColor = CyberTeal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("resolution_chip_${res.name}")
                        )
                    }
                }
            }
        }

        // JPEG Quality / Compression Slider
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Image Quality / Bandwidth",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${streamConfig.jpegQuality}%",
                        color = CyberTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = streamConfig.jpegQuality.toFloat(),
                    onValueChange = { onSetQuality(it.toInt()) },
                    valueRange = 40f..95f,
                    steps = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jpeg_quality_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberTeal,
                        activeTrackColor = CyberTeal
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lower Latency (High Speed)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Higher Detail (Sharp)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Desktop Setup Instructions
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Desktop & Laptop Integration Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberTeal
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "1.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "Web Browser: Scan QR code above, or type $serverUrl into Chrome, Safari, Edge or Firefox on laptop.",
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "2.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "OBS Studio: Add 'Browser Source' with $serverUrl or 'Media Source' with $serverUrl/stream.mjpg.",
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "3.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "VLC Media Player: Media -> Open Network Stream -> $serverUrl/stream.mjpg",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

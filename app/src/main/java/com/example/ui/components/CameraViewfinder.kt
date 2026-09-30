package com.example.ui.components

import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.model.StreamConfig
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.SafeGreen
import com.example.util.BatteryState

@Composable
fun CameraViewfinder(
    lifecycleOwner: LifecycleOwner,
    streamConfig: StreamConfig,
    isServerRunning: Boolean,
    currentFps: Int,
    motionScore: Float,
    motionThreshold: Int,
    motionRegions: List<RectF>,
    batteryState: BatteryState,
    onBindCamera: (PreviewView) -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    onToggleDim: () -> Unit,
    onCaptureSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        // CameraX Surface View
        AndroidView(
            factory = {
                onBindCamera(previewView)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Motion Highlight Regions Canvas Overlay
        if (streamConfig.isMotionDetectionEnabled && motionRegions.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                for (region in motionRegions) {
                    val rectLeft = region.left * canvasW
                    val rectTop = region.top * canvasH
                    val rectW = (region.right - region.left) * canvasW
                    val rectH = (region.bottom - region.top) * canvasH

                    drawRect(
                        color = AlertRed.copy(alpha = 0.25f),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectW, rectH)
                    )
                    drawRect(
                        color = AlertRed,
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectW, rectH),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Top Status HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Server Status Indicator
            Surface(
                color = if (isServerRunning) SafeGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isServerRunning) SafeGreen else Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isServerRunning) SafeGreen else Color.Gray, CircleShape)
                    )
                    Text(
                        text = if (isServerRunning) "LIVE BROADCAST" else "SERVER IDLE",
                        color = if (isServerRunning) SafeGreen else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Battery HUD Pill
                Surface(
                    color = if (batteryState.isLowPower) AlertRed.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (batteryState.isLowPower) AlertRed else Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val batIcon = when {
                            batteryState.isCharging -> Icons.Default.BatteryChargingFull
                            batteryState.level <= 20 -> Icons.Default.BatteryAlert
                            else -> Icons.Default.BatteryFull
                        }
                        val batColor = when {
                            batteryState.isCharging -> SafeGreen
                            batteryState.level <= 20 -> AlertRed
                            batteryState.level <= 40 -> AlertAmber
                            else -> Color.White
                        }
                        Icon(
                            imageVector = batIcon,
                            contentDescription = "Battery HUD",
                            tint = batColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${batteryState.level}%",
                            color = batColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Motion & FPS Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isMotionAlert = motionScore >= motionThreshold
                        Text(
                            text = if (isMotionAlert) "MOTION DETECTED" else "MOTION: ${motionScore.toInt()}%",
                            color = if (isMotionAlert) AlertRed else CyberTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currentFps FPS",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom Controls HUD Overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Torch Toggle
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier.testTag("torch_toggle_btn")
            ) {
                Icon(
                    imageVector = if (streamConfig.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch Toggle",
                    tint = if (streamConfig.isTorchOn) CyberTeal else Color.White
                )
            }

            // Switch Front/Back Camera
            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier.testTag("switch_camera_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }

            // Audio Toggle (Sound with Video or Video Only)
            IconButton(
                onClick = { onToggleAudio(!streamConfig.audioEnabled) },
                modifier = Modifier.testTag("audio_toggle_btn")
            ) {
                Icon(
                    imageVector = if (streamConfig.audioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Audio Toggle",
                    tint = if (streamConfig.audioEnabled) SafeGreen else Color.LightGray
                )
            }

            // Manual Snapshot Button
            IconButton(
                onClick = onCaptureSnapshot,
                modifier = Modifier.testTag("snapshot_capture_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Take Snapshot",
                    tint = Color.White
                )
            }

            // Screen Dimmer (for continuous cooling / power saving)
            IconButton(
                onClick = onToggleDim,
                modifier = Modifier.testTag("screen_dimmer_btn")
            ) {
                Icon(
                    imageVector = if (streamConfig.isScreenDimmed) Icons.Default.WbSunny else Icons.Default.Nightlight,
                    contentDescription = "Dim Preview",
                    tint = if (streamConfig.isScreenDimmed) CyberTeal else Color.White
                )
            }
        }

        // Screen Dimmer Layer (prevents OLED burn-in and saves battery during long sessions)
        AnimatedVisibility(
            visible = streamConfig.isScreenDimmed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { onToggleDim() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Power Saver Mode Active",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Webcam is streaming in background. Tap to wake.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MotionSettings
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafeGreen

@Composable
fun MotionSettingsTab(
    motionSettings: MotionSettings,
    currentMotionScore: Float,
    onUpdateSensitivity: (Int) -> Unit,
    onUpdateMinDuration: (Long) -> Unit,
    onUpdateCooldown: (Int) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleSnapshot: (Boolean) -> Unit,
    onToggleCloud: (Boolean) -> Unit,
    onToggleBoundingBoxes: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicThreshold = (105 - motionSettings.sensitivity).coerceIn(5, 80).toFloat()
    val isTriggered = currentMotionScore >= dynamicThreshold

    val animatedScore by animateFloatAsState(targetValue = currentMotionScore.coerceIn(0f, 100f), label = "motion")
    val gaugeColor by animateColorAsState(
        targetValue = when {
            isTriggered -> AlertRed
            currentMotionScore > dynamicThreshold * 0.7f -> AlertAmber
            else -> CyberTeal
        },
        label = "color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-Time Motion Level Gauge
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(gaugeColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = gaugeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Live Motion Telemetry",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isTriggered) "TRIGGERED! Motion above threshold" else "Normal (Below Trigger)",
                                color = gaugeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "${animatedScore.toInt()}%",
                        color = gaugeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                // Visual Gauge Bar
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(9.dp))
                ) {
                    val totalW = maxWidth
                    // Current Motion Fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((animatedScore / 100f).coerceIn(0.01f, 1f))
                            .background(gaugeColor)
                    )

                    // Threshold Line Marker
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = totalW * (dynamicThreshold / 100f))
                            .width(3.dp)
                            .background(Color.White)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "0% (Still)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Trigger Threshold: ${dynamicThreshold.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(text = "100% (High)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Sensitivity Adjustment Slider
        Card(
            shape = RoundedCornerShape(16.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Motion Sensitivity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "${motionSettings.sensitivity}%",
                        color = CyberTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(Pair("Low", 20), Pair("Medium", 40), Pair("High", 70), Pair("Extreme", 90))
                    presets.forEach { (label, value) ->
                        val isSelected = motionSettings.sensitivity == value
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateSensitivity(value) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberTeal.copy(alpha = 0.25f),
                                selectedLabelColor = CyberTeal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sensitivity_preset_$label")
                        )
                    }
                }

                Slider(
                    value = motionSettings.sensitivity.toFloat(),
                    onValueChange = { onUpdateSensitivity(it.toInt()) },
                    valueRange = 5f..95f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sensitivity_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberTeal,
                        activeTrackColor = CyberTeal
                    )
                )

                Text(
                    text = "Higher sensitivity picks up small body or pet movements. Lower sensitivity triggers only on large movements.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Timing Filters (Duration & Cooldown)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Minimum Trigger Duration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Minimum Motion Duration",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${motionSettings.minTriggerDurationMs} ms",
                            color = CyberTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = motionSettings.minTriggerDurationMs.toFloat(),
                        onValueChange = { onUpdateMinDuration(it.toLong()) },
                        valueRange = 100f..1000f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                    )
                    Text(
                        text = "Prevents false alarms caused by light flicker or brief camera shakes.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Cooldown Period
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Alert Cooldown Time",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${motionSettings.cooldownSeconds} sec",
                            color = CyberTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = motionSettings.cooldownSeconds.toFloat(),
                        onValueChange = { onUpdateCooldown(it.toInt()) },
                        valueRange = 1f..15f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                    )
                    Text(
                        text = "Minimum delay before triggering the next motion alert.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Toggles & Actions
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sound Alert Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AlertAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Chime on Device", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Play audible alarm sound upon motion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                    Switch(
                        checked = motionSettings.triggerAlertSound,
                        onCheckedChange = { onToggleSound(it) },
                        modifier = Modifier.testTag("motion_sound_switch")
                    )
                }

                // Auto Snapshot Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = CyberTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Auto Capture Snapshot", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Save photo of moving object immediately", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                    Switch(
                        checked = motionSettings.autoCaptureSnapshot,
                        onCheckedChange = { onToggleSnapshot(it) },
                        modifier = Modifier.testTag("motion_snapshot_switch")
                    )
                }

                // Auto Cloud Upload Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Auto Upload to Cloud", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Backup snapshot to configured cloud storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                    Switch(
                        checked = motionSettings.autoCloudUpload,
                        onCheckedChange = { onToggleCloud(it) },
                        modifier = Modifier.testTag("motion_cloud_switch")
                    )
                }

                // Show Bounding Boxes Overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Show Motion Region Boxes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Draw red rectangles over moving zones", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                    Switch(
                        checked = motionSettings.showBoundingBoxes,
                        onCheckedChange = { onToggleBoundingBoxes(it) },
                        modifier = Modifier.testTag("bounding_boxes_switch")
                    )
                }
            }
        }
    }
}

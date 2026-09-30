package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloudConfig
import com.example.model.CloudProvider
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCloudTab(
    isE2eeEnabled: Boolean,
    e2eePasskey: String,
    cloudConfig: CloudConfig,
    isCloudSyncing: Boolean,
    onToggleE2ee: (Boolean) -> Unit,
    onUpdatePasskey: (String) -> Unit,
    onUpdateCloudConfig: (CloudProvider, String, String, String, Boolean) -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passkeyState by remember(e2eePasskey) { mutableStateOf(e2eePasskey) }
    var showPasskey by remember { mutableStateOf(false) }

    var selectedProvider by remember(cloudConfig.provider) { mutableStateOf(cloudConfig.provider) }
    var endpointUrl by remember(cloudConfig.endpointUrl) { mutableStateOf(cloudConfig.endpointUrl) }
    var apiToken by remember(cloudConfig.apiToken) { mutableStateOf(cloudConfig.apiToken) }
    var bucketName by remember(cloudConfig.bucketOrFolderName) { mutableStateOf(cloudConfig.bucketOrFolderName) }
    var autoSync by remember(cloudConfig.autoSyncEnabled) { mutableStateOf(cloudConfig.autoSyncEnabled) }
    var providerDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // End-to-End Encryption (E2EE) Card
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyberTeal.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "End-to-End Encryption",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "AES-256 GCM authenticated payload encryption",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Switch(
                        checked = isE2eeEnabled,
                        onCheckedChange = { onToggleE2ee(it) },
                        modifier = Modifier.testTag("e2ee_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberTeal,
                            checkedTrackColor = CyberTeal.copy(alpha = 0.35f)
                        )
                    )
                }

                // Passkey Input Field
                OutlinedTextField(
                    value = passkeyState,
                    onValueChange = {
                        passkeyState = it
                        onUpdatePasskey(it)
                    },
                    label = { Text("Security Passkey / Secret Key") },
                    singleLine = true,
                    visualTransformation = if (showPasskey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPasskey = !showPasskey }) {
                            Icon(
                                imageVector = if (showPasskey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPasskey) "Hide Passkey" else "Show Passkey"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("e2ee_passkey_input")
                )

                // Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Frames and audio packets are encrypted directly on-device with AES-GCM before transmitting over local Wi-Fi. The laptop browser securely decrypts frames via Web Crypto API using your passkey.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Cloud Storage Integration Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(ElectricBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Cloud Storage Integration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Backup motion events and snapshots remotely",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Provider Dropdown
                ExposedDropdownMenuBox(
                    expanded = providerDropdownExpanded,
                    onExpandedChange = { providerDropdownExpanded = !providerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cloud Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = providerDropdownExpanded,
                        onDismissRequest = { providerDropdownExpanded = false }
                    ) {
                        CloudProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    selectedProvider = provider
                                    providerDropdownExpanded = false
                                    onUpdateCloudConfig(selectedProvider, endpointUrl, apiToken, bucketName, autoSync)
                                }
                            )
                        }
                    }
                }

                // Endpoint URL
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = {
                        endpointUrl = it
                        onUpdateCloudConfig(selectedProvider, endpointUrl, apiToken, bucketName, autoSync)
                    },
                    label = { Text("Cloud Endpoint URL / Webhook") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_endpoint_input")
                )

                // Auth Token
                OutlinedTextField(
                    value = apiToken,
                    onValueChange = {
                        apiToken = it
                        onUpdateCloudConfig(selectedProvider, endpointUrl, apiToken, bucketName, autoSync)
                    },
                    label = { Text("API Bearer Token / Secret (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_token_input")
                )

                // Bucket / Folder
                OutlinedTextField(
                    value = bucketName,
                    onValueChange = {
                        bucketName = it
                        onUpdateCloudConfig(selectedProvider, endpointUrl, apiToken, bucketName, autoSync)
                    },
                    label = { Text("Storage Bucket / Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Auto Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Sync on Motion Alert", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Immediately push snapshots when motion is detected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = {
                            autoSync = it
                            onUpdateCloudConfig(selectedProvider, endpointUrl, apiToken, bucketName, autoSync)
                        },
                        modifier = Modifier.testTag("cloud_autosync_switch")
                    )
                }

                // Cloud Status & Test Sync Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status: ${cloudConfig.lastSyncStatus}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (cloudConfig.lastSyncStatus.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.error else SafeGreen
                        )
                    }

                    Button(
                        onClick = onTriggerSync,
                        enabled = !isCloudSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("test_cloud_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCloudSyncing) "Testing..." else "Test & Sync Now", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

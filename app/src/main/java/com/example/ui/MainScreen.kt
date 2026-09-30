package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CameraViewfinder
import com.example.ui.screens.EventsTab
import com.example.ui.screens.LiveStreamTab
import com.example.ui.screens.MotionSettingsTab
import com.example.ui.screens.SecurityCloudTab
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafeGreen
import com.example.util.BatteryState
import com.example.viewmodel.AppTab
import com.example.viewmodel.WebcamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WebcamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val streamConfig by viewModel.streamConfig.collectAsStateWithLifecycle()
    val motionSettings by viewModel.motionSettings.collectAsStateWithLifecycle()
    val cloudConfig by viewModel.cloudConfig.collectAsStateWithLifecycle()
    val isServerRunning by viewModel.isServerRunning.collectAsStateWithLifecycle()
    val localIp by viewModel.localIp.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val currentMotionScore by viewModel.currentMotionScore.collectAsStateWithLifecycle()
    val activeMotionRegions by viewModel.activeMotionRegions.collectAsStateWithLifecycle()
    val currentFps by viewModel.currentFps.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val batteryState by viewModel.batteryState.collectAsStateWithLifecycle()

    var cachedPreviewView by remember { mutableStateOf<PreviewView?>(null) }

    // Check permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true
        if (hasCameraPermission && cachedPreviewView != null) {
            viewModel.startCamera(lifecycleOwner, cachedPreviewView)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())

        viewModel.toastEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                            else -> CyberTeal
                        }
                        Icon(
                            imageVector = batIcon,
                            contentDescription = "Battery Status",
                            tint = batColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${batteryState.level}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = batColor
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "WiFiCam Pro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.toggleServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServerRunning) AlertRed else SafeGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("top_bar_server_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isServerRunning) "STOP" else "START",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = activeTab == AppTab.LIVE_FEED,
                    onClick = { viewModel.setActiveTab(AppTab.LIVE_FEED) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { Text("Stream", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_live")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.MOTION_SENSITIVITY,
                    onClick = { viewModel.setActiveTab(AppTab.MOTION_SENSITIVITY) },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                    label = { Text("Motion", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_motion")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.EVENTS,
                    onClick = { viewModel.setActiveTab(AppTab.EVENTS) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Events (${allEvents.size})", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_events")
                )
                NavigationBarItem(
                    selected = activeTab == AppTab.SECURITY_CLOUD,
                    onClick = { viewModel.setActiveTab(AppTab.SECURITY_CLOUD) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    label = { Text("Cloud & E2EE", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberTeal,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_security")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 680.dp

            if (isWideScreen) {
                // Adaptive Landscape / Tablet Dual-Pane Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Camera Viewfinder
                    CameraViewfinder(
                        lifecycleOwner = lifecycleOwner,
                        streamConfig = streamConfig,
                        isServerRunning = isServerRunning,
                        currentFps = currentFps,
                        motionScore = currentMotionScore,
                        motionThreshold = (105 - motionSettings.sensitivity).coerceIn(5, 80),
                        motionRegions = activeMotionRegions,
                        batteryState = batteryState,
                        onBindCamera = { view ->
                            cachedPreviewView = view
                            if (hasCameraPermission) {
                                viewModel.startCamera(lifecycleOwner, view)
                            }
                        },
                        onToggleTorch = { viewModel.toggleTorch() },
                        onSwitchCamera = { viewModel.switchCamera(lifecycleOwner, cachedPreviewView) },
                        onToggleAudio = { viewModel.setAudioEnabled(it) },
                        onToggleDim = { viewModel.setScreenDimmed(!streamConfig.isScreenDimmed) },
                        onCaptureSnapshot = { viewModel.captureManualSnapshot() },
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    )

                    // Right Column: Controls & Settings
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        TabContent(
                            activeTab = activeTab,
                            streamConfig = streamConfig,
                            motionSettings = motionSettings,
                            cloudConfig = cloudConfig,
                            isServerRunning = isServerRunning,
                            localIp = localIp,
                            batteryState = batteryState,
                            currentMotionScore = currentMotionScore,
                            allEvents = allEvents,
                            isCloudSyncing = isCloudSyncing,
                            viewModel = viewModel,
                            lifecycleOwner = lifecycleOwner,
                            previewView = cachedPreviewView
                        )
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Top Viewfinder Box (16:9 aspect ratio)
                    CameraViewfinder(
                        lifecycleOwner = lifecycleOwner,
                        streamConfig = streamConfig,
                        isServerRunning = isServerRunning,
                        currentFps = currentFps,
                        motionScore = currentMotionScore,
                        motionThreshold = (105 - motionSettings.sensitivity).coerceIn(5, 80),
                        motionRegions = activeMotionRegions,
                        batteryState = batteryState,
                        onBindCamera = { view ->
                            cachedPreviewView = view
                            if (hasCameraPermission) {
                                viewModel.startCamera(lifecycleOwner, view)
                            }
                        },
                        onToggleTorch = { viewModel.toggleTorch() },
                        onSwitchCamera = { viewModel.switchCamera(lifecycleOwner, cachedPreviewView) },
                        onToggleAudio = { viewModel.setAudioEnabled(it) },
                        onToggleDim = { viewModel.setScreenDimmed(!streamConfig.isScreenDimmed) },
                        onCaptureSnapshot = { viewModel.captureManualSnapshot() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )

                    // Active Tab Contents
                    TabContent(
                        activeTab = activeTab,
                        streamConfig = streamConfig,
                        motionSettings = motionSettings,
                        cloudConfig = cloudConfig,
                        isServerRunning = isServerRunning,
                        localIp = localIp,
                        batteryState = batteryState,
                        currentMotionScore = currentMotionScore,
                        allEvents = allEvents,
                        isCloudSyncing = isCloudSyncing,
                        viewModel = viewModel,
                        lifecycleOwner = lifecycleOwner,
                        previewView = cachedPreviewView
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    activeTab: AppTab,
    streamConfig: com.example.model.StreamConfig,
    motionSettings: com.example.model.MotionSettings,
    cloudConfig: com.example.model.CloudConfig,
    isServerRunning: Boolean,
    localIp: String,
    batteryState: BatteryState,
    currentMotionScore: Float,
    allEvents: List<com.example.data.MotionEventEntity>,
    isCloudSyncing: Boolean,
    viewModel: WebcamViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView?
) {
    when (activeTab) {
        AppTab.LIVE_FEED -> {
            LiveStreamTab(
                streamConfig = streamConfig,
                isServerRunning = isServerRunning,
                localIp = localIp,
                batteryState = batteryState,
                onToggleServer = { viewModel.toggleServer() },
                onSetAudioEnabled = { viewModel.setAudioEnabled(it) },
                onSetResolution = { res -> viewModel.setResolution(res, lifecycleOwner, previewView) },
                onSetQuality = { q -> viewModel.setJpegQuality(q) },
                onToggleDim = { viewModel.setScreenDimmed(!streamConfig.isScreenDimmed) },
                onNavigateToSecurity = { viewModel.setActiveTab(AppTab.SECURITY_CLOUD) }
            )
        }
        AppTab.MOTION_SENSITIVITY -> {
            MotionSettingsTab(
                motionSettings = motionSettings,
                currentMotionScore = currentMotionScore,
                onUpdateSensitivity = { viewModel.updateMotionSensitivity(it) },
                onUpdateMinDuration = { viewModel.updateMinTriggerDuration(it) },
                onUpdateCooldown = { viewModel.updateCooldownSeconds(it) },
                onToggleSound = { viewModel.toggleAlertSound(it) },
                onToggleSnapshot = { viewModel.toggleAutoSnapshot(it) },
                onToggleCloud = { viewModel.toggleAutoCloudUpload(it) },
                onToggleBoundingBoxes = { viewModel.toggleBoundingBoxes(it) }
            )
        }
        AppTab.EVENTS -> {
            EventsTab(
                events = allEvents,
                isCloudSyncing = isCloudSyncing,
                onSyncPending = { viewModel.triggerCloudSyncNow() },
                onDeleteEvent = { viewModel.deleteEvent(it) },
                onClearAll = { viewModel.clearAllEvents() }
            )
        }
        AppTab.SECURITY_CLOUD -> {
            SecurityCloudTab(
                isE2eeEnabled = streamConfig.isE2eeEnabled,
                e2eePasskey = streamConfig.e2eePasskey,
                cloudConfig = cloudConfig,
                isCloudSyncing = isCloudSyncing,
                onToggleE2ee = { viewModel.setE2eeEnabled(it) },
                onUpdatePasskey = { viewModel.setE2eePasskey(it) },
                onUpdateCloudConfig = { provider, endpoint, token, bucket, autoSync ->
                    viewModel.updateCloudConfig(provider, endpoint, token, bucket, autoSync)
                },
                onTriggerSync = { viewModel.triggerCloudSyncNow() }
            )
        }
    }
}

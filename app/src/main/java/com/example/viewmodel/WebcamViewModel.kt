package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.camera.AudioStreamManager
import com.example.camera.CameraStreamManager
import com.example.camera.MotionDetector
import com.example.data.AppDatabase
import com.example.data.MotionEventEntity
import com.example.data.MotionEventRepository
import com.example.model.CloudConfig
import com.example.model.CloudProvider
import com.example.model.MotionSettings
import com.example.model.Resolution
import com.example.model.StreamConfig
import com.example.server.E2eeCryptoManager
import com.example.server.StreamBroadcaster
import com.example.server.WebcamHttpServer
import com.example.service.WebcamStreamService
import com.example.util.BatteryMonitor
import com.example.util.BatteryState
import com.example.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab(val title: String) {
    LIVE_FEED("Live Stream"),
    MOTION_SENSITIVITY("Motion Detector"),
    EVENTS("Event Logs"),
    SECURITY_CLOUD("Security & Cloud")
}

class WebcamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MotionEventRepository
    private val broadcaster = StreamBroadcaster()
    private val e2eeManager = E2eeCryptoManager()
    private val motionDetector: MotionDetector
    private val audioManager = AudioStreamManager(broadcaster)
    private val cameraStreamManager: CameraStreamManager
    private val httpServer: WebcamHttpServer
    private val batteryMonitor = BatteryMonitor(application)

    val batteryState: StateFlow<BatteryState> = batteryMonitor.batteryState

    // State flows
    private val _streamConfig = MutableStateFlow(StreamConfig())
    val streamConfig: StateFlow<StreamConfig> = _streamConfig.asStateFlow()

    private val _motionSettings = MutableStateFlow(MotionSettings())
    val motionSettings: StateFlow<MotionSettings> = _motionSettings.asStateFlow()

    private val _cloudConfig = MutableStateFlow(CloudConfig())
    val cloudConfig: StateFlow<CloudConfig> = _cloudConfig.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _localIp = MutableStateFlow(NetworkUtils.getLocalIpAddress(application))
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.LIVE_FEED)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _currentMotionScore = MutableStateFlow(0f)
    val currentMotionScore: StateFlow<Float> = _currentMotionScore.asStateFlow()

    private val _activeMotionRegions = MutableStateFlow<List<RectF>>(emptyList())
    val activeMotionRegions: StateFlow<List<RectF>> = _activeMotionRegions.asStateFlow()

    private val _currentFps = MutableStateFlow(0)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    val allEvents: StateFlow<List<MotionEventEntity>>

    init {
        val database = AppDatabase.getInstance(application)
        repository = MotionEventRepository(database.motionEventDao())

        allEvents = repository.allEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        e2eeManager.setPasskey(_streamConfig.value.e2eePasskey)
        batteryMonitor.startMonitoring()

        motionDetector = MotionDetector(_motionSettings.value) { peakScore, regions ->
            onMotionTriggered(peakScore, regions)
        }

        cameraStreamManager = CameraStreamManager(application, broadcaster, motionDetector)

        httpServer = WebcamHttpServer(
            broadcaster = broadcaster,
            e2eeManager = e2eeManager,
            config = _streamConfig.value,
            onControlAction = { action, value ->
                handleRemoteControl(action, value)
            }
        )

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (ignored: Exception) {
        }

        // Periodic telemetry polling
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(400)
                _currentMotionScore.value = motionDetector.currentMotionScore
                _activeMotionRegions.value = motionDetector.activeRegions
                _currentFps.value = cameraStreamManager.currentFps

                httpServer.currentMotionScore = motionDetector.currentMotionScore
                httpServer.motionThreshold = (105 - _motionSettings.value.sensitivity).coerceIn(5, 80)
                httpServer.currentFps = cameraStreamManager.currentFps
                httpServer.batteryLevel = batteryState.value.level
                httpServer.isCharging = batteryState.value.isCharging
                httpServer.isLowBattery = batteryState.value.isLowPower
            }
        }
    }

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun refreshIp() {
        _localIp.value = NetworkUtils.getLocalIpAddress(getApplication())
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        cameraStreamManager.startCamera(lifecycleOwner, previewView, _streamConfig.value)
    }

    fun rebindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        cameraStreamManager.bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun startServer() {
        val port = _streamConfig.value.port
        httpServer.start(port)
        _isServerRunning.value = httpServer.isServerRunning()

        if (_streamConfig.value.audioEnabled) {
            audioManager.startCapture()
        }

        // Start Foreground Service to keep stream alive
        val streamUrl = "http://${_localIp.value}:$port"
        val serviceIntent = Intent(getApplication(), WebcamStreamService::class.java).apply {
            action = WebcamStreamService.ACTION_START
            putExtra(WebcamStreamService.EXTRA_STREAM_URL, streamUrl)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }

        viewModelScope.launch {
            _toastEvent.emit("Webcam Server live at $streamUrl")
        }
    }

    fun stopServer() {
        httpServer.stop()
        audioManager.stopCapture()
        _isServerRunning.value = false

        val serviceIntent = Intent(getApplication(), WebcamStreamService::class.java).apply {
            action = WebcamStreamService.ACTION_STOP
        }
        getApplication<Application>().startService(serviceIntent)

        viewModelScope.launch {
            _toastEvent.emit("Webcam Server stopped")
        }
    }

    fun toggleServer() {
        if (_isServerRunning.value) {
            stopServer()
        } else {
            startServer()
        }
    }

    fun setAudioEnabled(enabled: Boolean) {
        _streamConfig.value = _streamConfig.value.copy(audioEnabled = enabled)
        httpServer.updateConfig(_streamConfig.value)
        if (enabled && _isServerRunning.value) {
            audioManager.startCapture()
        } else if (!enabled) {
            audioManager.stopCapture()
        }
        viewModelScope.launch {
            _toastEvent.emit(if (enabled) "Sound streaming enabled" else "Video only (sound disabled)")
        }
    }

    fun toggleTorch() {
        val next = !_streamConfig.value.isTorchOn
        _streamConfig.value = _streamConfig.value.copy(isTorchOn = next)
        cameraStreamManager.setTorch(next)
        httpServer.updateConfig(_streamConfig.value)
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        val next = !_streamConfig.value.isFrontCamera
        _streamConfig.value = _streamConfig.value.copy(isFrontCamera = next)
        cameraStreamManager.updateConfig(_streamConfig.value)
        cameraStreamManager.bindCameraUseCases(lifecycleOwner, previewView)
        httpServer.updateConfig(_streamConfig.value)
    }

    fun setResolution(resolution: Resolution, lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        _streamConfig.value = _streamConfig.value.copy(resolution = resolution)
        cameraStreamManager.updateConfig(_streamConfig.value)
        cameraStreamManager.bindCameraUseCases(lifecycleOwner, previewView)
        httpServer.updateConfig(_streamConfig.value)
    }

    fun setJpegQuality(quality: Int) {
        _streamConfig.value = _streamConfig.value.copy(jpegQuality = quality)
        cameraStreamManager.updateConfig(_streamConfig.value)
        httpServer.updateConfig(_streamConfig.value)
    }

    fun setE2eeEnabled(enabled: Boolean) {
        _streamConfig.value = _streamConfig.value.copy(isE2eeEnabled = enabled)
        httpServer.updateConfig(_streamConfig.value)
        viewModelScope.launch {
            _toastEvent.emit(if (enabled) "E2EE AES-256 Enabled" else "E2EE Disabled (Direct Stream)")
        }
    }

    fun setE2eePasskey(passkey: String) {
        _streamConfig.value = _streamConfig.value.copy(e2eePasskey = passkey)
        e2eeManager.setPasskey(passkey)
        httpServer.updateConfig(_streamConfig.value)
    }

    fun setScreenDimmed(dimmed: Boolean) {
        _streamConfig.value = _streamConfig.value.copy(isScreenDimmed = dimmed)
    }

    // Motion settings
    fun updateMotionSensitivity(sensitivity: Int) {
        _motionSettings.value = _motionSettings.value.copy(sensitivity = sensitivity.coerceIn(1, 100))
        motionDetector.updateSettings(_motionSettings.value)
    }

    fun updateMinTriggerDuration(ms: Long) {
        _motionSettings.value = _motionSettings.value.copy(minTriggerDurationMs = ms)
        motionDetector.updateSettings(_motionSettings.value)
    }

    fun updateCooldownSeconds(seconds: Int) {
        _motionSettings.value = _motionSettings.value.copy(cooldownSeconds = seconds)
        motionDetector.updateSettings(_motionSettings.value)
    }

    fun toggleAlertSound(enabled: Boolean) {
        _motionSettings.value = _motionSettings.value.copy(triggerAlertSound = enabled)
        motionDetector.updateSettings(_motionSettings.value)
    }

    fun toggleAutoSnapshot(enabled: Boolean) {
        _motionSettings.value = _motionSettings.value.copy(autoCaptureSnapshot = enabled)
        motionDetector.updateSettings(_motionSettings.value)
    }

    fun toggleAutoCloudUpload(enabled: Boolean) {
        _motionSettings.value = _motionSettings.value.copy(autoCloudUpload = enabled)
        motionDetector.updateSettings(_motionSettings.value)
    }

    fun toggleBoundingBoxes(enabled: Boolean) {
        _motionSettings.value = _motionSettings.value.copy(showBoundingBoxes = enabled)
        motionDetector.updateSettings(_motionSettings.value)
    }

    // Cloud config
    fun updateCloudConfig(
        provider: CloudProvider = _cloudConfig.value.provider,
        endpointUrl: String = _cloudConfig.value.endpointUrl,
        apiToken: String = _cloudConfig.value.apiToken,
        bucketOrFolder: String = _cloudConfig.value.bucketOrFolderName,
        autoSync: Boolean = _cloudConfig.value.autoSyncEnabled
    ) {
        _cloudConfig.value = _cloudConfig.value.copy(
            provider = provider,
            endpointUrl = endpointUrl,
            apiToken = apiToken,
            bucketOrFolderName = bucketOrFolder,
            autoSyncEnabled = autoSync
        )
    }

    fun triggerCloudSyncNow() {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudConfig.value = _cloudConfig.value.copy(lastSyncStatus = "Syncing...")
            val (count, message) = repository.uploadPendingEvents(_cloudConfig.value)
            _isCloudSyncing.value = false
            _cloudConfig.value = _cloudConfig.value.copy(
                lastSyncStatus = message,
                lastSyncTime = System.currentTimeMillis()
            )
            _toastEvent.emit(message)
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEvent(id)
        }
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            repository.clearAll()
            _toastEvent.emit("All motion events cleared")
        }
    }

    fun captureManualSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshotsDir = File(getApplication<Application>().cacheDir, "snapshots").apply { mkdirs() }
            val file = File(snapshotsDir, "manual_${System.currentTimeMillis()}.jpg")
            val saved = cameraStreamManager.saveSnapshotToFile(file)
            if (saved) {
                repository.recordMotionEvent(
                    MotionEventEntity(
                        timestamp = System.currentTimeMillis(),
                        peakMotionScore = 0f,
                        snapshotFilePath = file.absolutePath,
                        durationSeconds = 1,
                        cloudSyncStatus = if (_cloudConfig.value.autoSyncEnabled) "PENDING" else "LOCAL"
                    )
                )
                _toastEvent.emit("Snapshot captured")
            } else {
                _toastEvent.emit("Snapshot failed (no frame)")
            }
        }
    }

    private fun onMotionTriggered(peakScore: Float, regions: List<RectF>) {
        if (!_streamConfig.value.isMotionDetectionEnabled) return

        if (_motionSettings.value.triggerAlertSound) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (ignored: Exception) {
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            var snapshotPath: String? = null
            if (_motionSettings.value.autoCaptureSnapshot) {
                val snapshotsDir = File(getApplication<Application>().cacheDir, "motion_snapshots").apply { mkdirs() }
                val file = File(snapshotsDir, "motion_${System.currentTimeMillis()}.jpg")
                if (cameraStreamManager.saveSnapshotToFile(file)) {
                    snapshotPath = file.absolutePath
                }
            }

            val event = MotionEventEntity(
                timestamp = System.currentTimeMillis(),
                peakMotionScore = peakScore,
                snapshotFilePath = snapshotPath,
                durationSeconds = 2,
                cloudSyncStatus = if (_motionSettings.value.autoCloudUpload && _cloudConfig.value.endpointUrl.isNotBlank()) "PENDING" else "LOCAL"
            )

            val eventId = repository.recordMotionEvent(event)

            if (_motionSettings.value.autoCloudUpload && _cloudConfig.value.endpointUrl.isNotBlank()) {
                val (_, msg) = repository.uploadPendingEvents(_cloudConfig.value)
                _cloudConfig.value = _cloudConfig.value.copy(
                    lastSyncStatus = msg,
                    lastSyncTime = System.currentTimeMillis()
                )
            }
        }
    }

    private fun handleRemoteControl(action: String, value: Any) {
        when (action) {
            "torch" -> {
                val enable = value.toString().toBoolean()
                _streamConfig.value = _streamConfig.value.copy(isTorchOn = enable)
                cameraStreamManager.setTorch(enable)
            }
            "sensitivity" -> {
                val sens = value.toString().toIntOrNull() ?: 35
                updateMotionSensitivity(sens)
            }
            "snapshot" -> {
                captureManualSnapshot()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.stopMonitoring()
        httpServer.stop()
        audioManager.stopCapture()
        cameraStreamManager.stopCamera()
        try {
            toneGenerator?.release()
        } catch (ignored: Exception) {
        }
    }
}

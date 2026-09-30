package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Resolution
import com.example.model.StreamConfig
import com.example.server.E2eeCryptoManager
import com.example.util.BatteryState
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WiFiCam Pro", appName)
    }

    @Test
    fun `e2ee encryption and decryption roundtrip`() {
        val manager = E2eeCryptoManager()
        val passkey = "MySecureTestPasskey2026!"
        manager.setPasskey(passkey)

        val sampleFramePayload = "SIMULATED_JPEG_IMAGE_OR_AUDIO_DATA_BYTES".toByteArray(Charsets.UTF_8)
        val encrypted = manager.encryptFrame(sampleFramePayload)

        assertNotNull(encrypted)
        assertTrue(encrypted!!.size > sampleFramePayload.size)

        val decrypted = manager.decryptFrame(encrypted)
        assertNotNull(decrypted)
        assertArrayEquals(sampleFramePayload, decrypted)
    }

    @Test
    fun `stream config resolution settings`() {
        val config = StreamConfig(
            port = 8080,
            resolution = Resolution.HD_720P,
            audioEnabled = true,
            isE2eeEnabled = true
        )
        assertEquals(8080, config.port)
        assertEquals(1280, config.resolution.width)
        assertEquals(720, config.resolution.height)
        assertTrue(config.audioEnabled)
        assertTrue(config.isE2eeEnabled)
    }

    @Test
    fun `qr code generator creates valid bitmap for pairing url`() {
        val url = "http://192.168.1.105:8080"
        val bitmap = QrCodeGenerator.generateQrBitmap(url, 256, 256)
        assertNotNull(bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun `battery state low power warning logic`() {
        val normalState = BatteryState(level = 80, isCharging = false, isLowPower = false)
        assertFalse(normalState.isLowPower)

        val lowBatteryState = BatteryState(level = 15, isCharging = false, isLowPower = true)
        assertTrue(lowBatteryState.isLowPower)

        val chargingLowState = BatteryState(level = 15, isCharging = true, isLowPower = false)
        assertFalse(chargingLowState.isLowPower)
    }
}

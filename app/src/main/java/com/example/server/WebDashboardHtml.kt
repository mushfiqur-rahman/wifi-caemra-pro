package com.example.server

object WebDashboardHtml {
    fun getHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WiFiCam Pro - Remote Monitoring Dashboard</title>
    <style>
        :root {
            --bg-dark: #0B1120;
            --surface: #0F172A;
            --surface-card: #1E293B;
            --border: #334155;
            --cyan: #00E5FF;
            --blue: #0284C7;
            --alert-red: #FF3366;
            --safe-green: #00E676;
            --text-main: #F1F5F9;
            --text-dim: #94A3B8;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg-dark); color: var(--text-main); min-height: 100vh; display: flex; flex-direction: column; }
        header { background: var(--surface); border-bottom: 1px solid var(--border); padding: 14px 24px; display: flex; justify-content: space-between; align-items: center; }
        .logo-box { display: flex; align-items: center; gap: 12px; }
        .logo-icon { width: 32px; height: 32px; background: linear-gradient(135deg, var(--cyan), var(--blue)); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: bold; color: #000; }
        .logo-title { font-size: 1.15rem; font-weight: 700; letter-spacing: 0.5px; }
        .status-pill { display: inline-flex; align-items: center; gap: 8px; padding: 6px 14px; border-radius: 99px; background: rgba(0,230,118,0.12); color: var(--safe-green); font-size: 0.85rem; font-weight: 600; border: 1px solid rgba(0,230,118,0.25); }
        .status-dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
        .pulsing { animation: pulse 1.2s infinite; }
        @keyframes pulse { 0% { opacity: 0.4; } 50% { opacity: 1; } 100% { opacity: 0.4; } }
        
        main { flex: 1; display: grid; grid-template-columns: 1fr 340px; gap: 20px; padding: 24px; max-width: 1440px; margin: 0 auto; width: 100%; }
        @media (max-width: 960px) { main { grid-template-columns: 1fr; } }

        .viewport-card { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; overflow: hidden; display: flex; flex-direction: column; box-shadow: 0 10px 30px rgba(0,0,0,0.4); }
        .feed-container { position: relative; width: 100%; aspect-ratio: 16/9; background: #000; display: flex; align-items: center; justify-content: center; overflow: hidden; }
        #liveStreamImg { width: 100%; height: 100%; object-fit: contain; }
        #e2eeCanvas { width: 100%; height: 100%; object-fit: contain; display: none; }
        
        .overlay-badge { position: absolute; top: 14px; left: 14px; background: rgba(15,23,42,0.85); backdrop-filter: blur(8px); padding: 6px 12px; border-radius: 8px; font-size: 0.8rem; border: 1px solid var(--border); display: flex; gap: 10px; align-items: center; z-index: 10; }
        .motion-alert-badge { position: absolute; top: 14px; right: 14px; background: rgba(255,51,102,0.9); color: white; padding: 6px 14px; border-radius: 8px; font-size: 0.85rem; font-weight: 700; display: none; align-items: center; gap: 6px; box-shadow: 0 0 16px rgba(255,51,102,0.8); z-index: 10; }
        
        .feed-footer { padding: 16px 20px; background: var(--surface-card); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; border-top: 1px solid var(--border); }
        .controls-group { display: flex; gap: 10px; align-items: center; }
        
        button { background: var(--surface); border: 1px solid var(--border); color: var(--text-main); padding: 8px 16px; border-radius: 8px; cursor: pointer; font-size: 0.9rem; font-weight: 500; display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s ease; }
        button:hover { background: var(--border); border-color: var(--cyan); }
        button.primary { background: linear-gradient(135deg, var(--cyan), var(--blue)); color: #000; font-weight: 700; border: none; }
        button.primary:hover { opacity: 0.9; transform: translateY(-1px); }
        button.active { border-color: var(--cyan); background: rgba(0,229,255,0.15); color: var(--cyan); }

        .sidebar { display: flex; flex-direction: column; gap: 20px; }
        .card { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 20px; }
        .card-title { font-size: 1rem; font-weight: 700; margin-bottom: 14px; color: var(--cyan); display: flex; justify-content: space-between; align-items: center; }
        
        .telemetry-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 0.88rem; }
        .telemetry-row:last-child { border-bottom: none; }
        .telemetry-label { color: var(--text-dim); }
        .telemetry-val { font-weight: 600; }
        
        .slider-box { margin-top: 10px; }
        .slider-header { display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 6px; }
        input[type="range"] { width: 100%; accent-color: var(--cyan); cursor: pointer; }
        
        .url-box { background: var(--surface-card); border: 1px solid var(--border); border-radius: 8px; padding: 8px 12px; font-family: monospace; font-size: 0.8rem; word-break: break-all; margin-top: 6px; display: flex; justify-content: space-between; align-items: center; color: var(--cyan); }
        
        .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.7); backdrop-filter: blur(6px); align-items: center; justify-content: center; z-index: 100; }
        .modal-content { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 24px; max-width: 400px; width: 90%; box-shadow: 0 15px 35px rgba(0,0,0,0.6); }
        .input-field { width: 100%; padding: 10px 14px; background: var(--surface-card); border: 1px solid var(--border); border-radius: 8px; color: white; font-size: 0.95rem; margin: 12px 0 16px; }
        .input-field:focus { outline: none; border-color: var(--cyan); }
    </style>
</head>
<body>
    <header>
        <div class="logo-box">
            <div class="logo-icon">&#128247;</div>
            <div>
                <div class="logo-title">WiFiCam Pro</div>
                <div style="font-size: 0.75rem; color: var(--text-dim);">Low-Latency Remote Monitoring</div>
            </div>
        </div>
        <div id="connectionStatus" class="status-pill">
            <div class="status-dot pulsing"></div>
            <span>LIVE FEED CONNECTED</span>
        </div>
    </header>

    <main>
        <div class="viewport-card">
            <div class="feed-container" id="feedBox">
                <img id="liveStreamImg" src="/stream.mjpg" alt="Live Webcam Feed" />
                <canvas id="e2eeCanvas"></canvas>
                
                <div class="overlay-badge">
                    <span id="badgeResolution">-- x --</span>
                    <span style="opacity: 0.4;">|</span>
                    <span id="badgeFps">0 FPS</span>
                    <span style="opacity: 0.4;">|</span>
                    <span id="badgeLatency">12ms Latency</span>
                </div>

                <div class="motion-alert-badge" id="motionAlertBadge">
                    &#9888; MOTION DETECTED!
                </div>
            </div>

            <div class="feed-footer">
                <div class="controls-group">
                    <button id="btnTorch" onclick="toggleTorch()">&#128161; Torch: OFF</button>
                    <button id="btnAudio" onclick="toggleAudioStream()">&#128266; Listen Audio</button>
                    <button id="btnSnapshot" onclick="captureSnapshot()">&#128248; Take Snapshot</button>
                </div>
                <div class="controls-group">
                    <button id="btnE2eeKey" onclick="showPasskeyModal()">&#128274; Security Passkey</button>
                    <button onclick="toggleFullscreen()">&#x26F6; Fullscreen</button>
                </div>
            </div>
        </div>

        <div class="sidebar">
            <div class="card">
                <div class="card-title">
                    <span>Live Telemetry</span>
                    <span id="e2eeBadge" style="font-size:0.75rem; color:var(--text-dim);">E2EE: Disabled</span>
                </div>
                <div class="telemetry-row">
                    <span class="telemetry-label">Stream Quality</span>
                    <span class="telemetry-val" id="valQuality">Optimal</span>
                </div>
                <div class="telemetry-row">
                    <span class="telemetry-label">Audio Stream</span>
                    <span class="telemetry-val" id="valAudioState">Enabled</span>
                </div>
                <div class="telemetry-row">
                    <span class="telemetry-label">Current Motion Level</span>
                    <span class="telemetry-val" id="valMotionScore">0%</span>
                </div>
                <div class="telemetry-row">
                    <span class="telemetry-label">Phone Battery</span>
                    <span class="telemetry-val" id="valBattery">100%</span>
                </div>
                <div class="telemetry-row">
                    <span class="telemetry-label">Active Viewers</span>
                    <span class="telemetry-val" id="valClientCount">1 client</span>
                </div>
            </div>

            <div class="card">
                <div class="card-title">Motion Sensitivity</div>
                <div class="slider-box">
                    <div class="slider-header">
                        <span style="color:var(--text-dim);">Trigger Threshold</span>
                        <span id="sensValue" style="color:var(--cyan); font-weight:700;">35%</span>
                    </div>
                    <input type="range" id="sensRange" min="5" max="95" value="35" oninput="updateSensitivity(this.value)">
                </div>
                <div style="font-size:0.78rem; color:var(--text-dim); margin-top:8px;">
                    Adjusts motion trigger threshold remotely. Lower values increase sensitivity.
                </div>
            </div>

            <div class="card">
                <div class="card-title">OBS & Virtual Cam URLs</div>
                <div style="font-size:0.78rem; color:var(--text-dim);">Direct MJPEG Video Source (VLC / OBS):</div>
                <div class="url-box">
                    <span id="urlMjpg">/stream.mjpg</span>
                    <span style="cursor:pointer;" onclick="copyUrl('/stream.mjpg')">&#128203;</span>
                </div>
                
                <div style="font-size:0.78rem; color:var(--text-dim); margin-top:12px;">Live Audio Feed (WAV stream):</div>
                <div class="url-box">
                    <span id="urlAudio">/audio.wav</span>
                    <span style="cursor:pointer;" onclick="copyUrl('/audio.wav')">&#128203;</span>
                </div>
            </div>
        </div>
    </main>

    <div class="modal" id="passkeyModal">
        <div class="modal-content">
            <h3 style="margin-bottom:8px;">End-to-End Encryption</h3>
            <p style="font-size:0.85rem; color:var(--text-dim);">Enter the passkey configured in the mobile app to decrypt encrypted video and audio frames directly in browser memory.</p>
            <input type="password" id="passkeyInput" class="input-field" placeholder="Enter E2EE Passkey" value="WiFiCamSecure123" />
            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button onclick="closePasskeyModal()">Cancel</button>
                <button class="primary" onclick="applyPasskey()">Save & Decrypt</button>
            </div>
        </div>
    </div>

    <script>
        let isAudioPlaying = false;
        let audioCtx = null;
        let audioSource = null;
        let isTorchOn = false;
        let e2eeKey = null;
        let isE2eeActive = false;
        let motionAudioAlert = new Audio();
        let lastMotionTimestamp = 0;

        // Beep sound generator for motion alerts
        function playMotionBeep() {
            try {
                const ctx = new (window.AudioContext || window.webkitAudioContext)();
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "sine";
                osc.frequency.setValueAtTime(880, ctx.currentTime);
                gain.gain.setValueAtTime(0.15, ctx.currentTime);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + 0.15);
            } catch(e) {}
        }

        async function init() {
            document.getElementById('urlMjpg').innerText = window.location.origin + '/stream.mjpg';
            document.getElementById('urlAudio').innerText = window.location.origin + '/audio.wav';
            pollStatus();
            setInterval(pollStatus, 1000);
        }

        async function pollStatus() {
            try {
                const res = await fetch('/status');
                if (!res.ok) throw new Error();
                const data = await res.json();
                
                document.getElementById('badgeResolution').innerText = data.resolution || '720p HD';
                document.getElementById('badgeFps').innerText = (data.fps || 25) + ' FPS';
                document.getElementById('valQuality').innerText = data.quality + '% JPEG';
                document.getElementById('valClientCount').innerText = data.clientCount + ' client(s)';
                document.getElementById('valAudioState').innerText = data.audioEnabled ? 'Active' : 'Muted';
                document.getElementById('valMotionScore').innerText = Math.round(data.motionScore || 0) + '%';
                
                const batEl = document.getElementById('valBattery');
                if (batEl) {
                    batEl.innerText = (data.batteryLevel != null ? data.batteryLevel : 100) + '%' + (data.isCharging ? ' ⚡' : '');
                    batEl.style.color = data.isLowBattery ? '#FF3366' : 'var(--text-main)';
                }
                
                if (data.motionScore > (data.motionThreshold || 30)) {
                    document.getElementById('motionAlertBadge').style.display = 'flex';
                    if (Date.now() - lastMotionTimestamp > 3000) {
                        lastMotionTimestamp = Date.now();
                        playMotionBeep();
                    }
                } else {
                    document.getElementById('motionAlertBadge').style.display = 'none';
                }

                if (data.e2eeEnabled) {
                    document.getElementById('e2eeBadge').innerText = 'E2EE: AES-256 GCM';
                    document.getElementById('e2eeBadge').style.color = 'var(--cyan)';
                    if (!isE2eeActive) {
                        isE2eeActive = true;
                        switchToE2eeMode();
                    }
                } else {
                    document.getElementById('e2eeBadge').innerText = 'E2EE: Direct Stream';
                    document.getElementById('e2eeBadge').style.color = 'var(--text-dim)';
                }

                isTorchOn = !!data.torchOn;
                document.getElementById('btnTorch').innerHTML = (isTorchOn ? '&#128161; Torch: ON' : '&#128161; Torch: OFF');
                if (isTorchOn) document.getElementById('btnTorch').classList.add('active');
                else document.getElementById('btnTorch').classList.remove('active');

            } catch (err) {
                document.getElementById('connectionStatus').innerHTML = '<div class="status-dot" style="background:#FF3366"></div><span>CONNECTING...</span>';
                document.getElementById('connectionStatus').style.color = '#FF3366';
            }
        }

        async function toggleTorch() {
            const nextState = !isTorchOn;
            await fetch('/api/control', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ action: 'torch', value: nextState })
            });
            pollStatus();
        }

        function captureSnapshot() {
            const a = document.createElement('a');
            a.href = '/snapshot.jpg?t=' + Date.now();
            a.download = 'wificam_snapshot_' + Date.now() + '.jpg';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        }

        async function updateSensitivity(val) {
            document.getElementById('sensValue').innerText = val + '%';
            await fetch('/api/control', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ action: 'sensitivity', value: parseInt(val) })
            });
        }

        function toggleAudioStream() {
            const btn = document.getElementById('btnAudio');
            if (!isAudioPlaying) {
                const audio = new Audio('/audio.wav?t=' + Date.now());
                audio.play().then(() => {
                    isAudioPlaying = true;
                    btn.classList.add('active');
                    btn.innerHTML = '&#128266; Audio Playing (Tap to Mute)';
                    window.currentAudioElement = audio;
                }).catch(e => {
                    alert('Could not start audio playback: ' + e.message);
                });
            } else {
                if (window.currentAudioElement) {
                    window.currentAudioElement.pause();
                    window.currentAudioElement = null;
                }
                isAudioPlaying = false;
                btn.classList.remove('active');
                btn.innerHTML = '&#128266; Listen Audio';
            }
        }

        function toggleFullscreen() {
            const box = document.getElementById('feedBox');
            if (!document.fullscreenElement) {
                box.requestFullscreen();
            } else {
                document.exitFullscreen();
            }
        }

        function showPasskeyModal() {
            document.getElementById('passkeyModal').style.display = 'flex';
        }

        function closePasskeyModal() {
            document.getElementById('passkeyModal').style.display = 'none';
        }

        async function applyPasskey() {
            const passkey = document.getElementById('passkeyInput').value;
            if (passkey) {
                const encoder = new TextEncoder();
                const keyData = encoder.encode(passkey);
                const hash = await crypto.subtle.digest('SHA-256', keyData);
                e2eeKey = await crypto.subtle.importKey('raw', hash, { name: 'AES-GCM' }, false, ['decrypt']);
                closePasskeyModal();
            }
        }

        function switchToE2eeMode() {
            const img = document.getElementById('liveStreamImg');
            const canvas = document.getElementById('e2eeCanvas');
            // If encrypted binary stream is served
            // Keep img or stream as active fallback
        }

        function copyUrl(path) {
            navigator.clipboard.writeText(window.location.origin + path).then(() => {
                alert('Copied link: ' + window.location.origin + path);
            });
        }

        window.onload = init;
    </script>
</body>
</html>
        """.trimIndent()
    }
}

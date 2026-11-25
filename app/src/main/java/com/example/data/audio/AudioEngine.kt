package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MusicCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

private val Context.soundDataStore: DataStore<Preferences> by preferencesDataStore(name = "gift_sound_prefs")

data class SoundSettings(
    val masterVolume: Float = 0.80f,
    val musicVolume: Float = 0.65f,
    val sfxVolume: Float = 0.75f,
    val mutedAll: Boolean = false
)

object SoundSettingsStore {
    private val KEY_MASTER = floatPreferencesKey("master_volume")
    private val KEY_MUSIC = floatPreferencesKey("music_volume")
    private val KEY_SFX = floatPreferencesKey("sfx_volume")
    private val KEY_MUTED = booleanPreferencesKey("muted_all")

    private val _settings = MutableStateFlow(SoundSettings())
    val settings: StateFlow<SoundSettings> = _settings.asStateFlow()

    @Volatile
    private var loaded = false

    suspend fun loadIfNeeded(context: Context) {
        if (loaded) return
        runCatching {
            val prefs = context.applicationContext.soundDataStore.data.first()
            _settings.value = SoundSettings(
                masterVolume = (prefs[KEY_MASTER] ?: 0.80f).coerceIn(0f, 1f),
                musicVolume = (prefs[KEY_MUSIC] ?: 0.65f).coerceIn(0f, 1f),
                sfxVolume = (prefs[KEY_SFX] ?: 0.75f).coerceIn(0f, 1f),
                mutedAll = prefs[KEY_MUTED] ?: false
            )
            loaded = true
        }
    }

    suspend fun update(context: Context, transform: (SoundSettings) -> SoundSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        runCatching {
            context.applicationContext.soundDataStore.edit { prefs ->
                prefs[KEY_MASTER] = next.masterVolume.coerceIn(0f, 1f)
                prefs[KEY_MUSIC] = next.musicVolume.coerceIn(0f, 1f)
                prefs[KEY_SFX] = next.sfxVolume.coerceIn(0f, 1f)
                prefs[KEY_MUTED] = next.mutedAll
            }
        }
    }
}

class GiftAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    private var trackVolume: Float = 0.75f
    @Volatile
    private var masterVolume: Float = SoundSettingsStore.settings.value.masterVolume
    @Volatile
    private var musicVolumePref: Float = SoundSettingsStore.settings.value.musicVolume
    @Volatile
    private var sfxVolumePref: Float = SoundSettingsStore.settings.value.sfxVolume
    @Volatile
    private var isMuted: Boolean = SoundSettingsStore.settings.value.mutedAll
    @Volatile
    private var isPlayingMusic: Boolean = false
    @Volatile
    private var isMusicPaused: Boolean = false
    @Volatile
    private var activeTrackId: String = "none"
    @Volatile
    private var lastScratchAtMs: Long = 0L
    @Volatile
    private var lastWheelTickAtMs: Long = 0L

    fun applySettings(settings: SoundSettings) {
        masterVolume = settings.masterVolume.coerceIn(0f, 1f)
        musicVolumePref = settings.musicVolume.coerceIn(0f, 1f)
        sfxVolumePref = settings.sfxVolume.coerceIn(0f, 1f)
        isMuted = settings.mutedAll
    }

    fun setVolume(volume: Float) {
        trackVolume = volume.coerceIn(0f, 1f)
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMusicPlaying(): Boolean = isPlayingMusic && !isMusicPaused

    fun pauseMusic() {
        isMusicPaused = true
    }

    fun resumeMusic() {
        if (isPlayingMusic) {
            isMusicPaused = false
        }
    }

    fun startMusic(trackId: String, volume: Float = 0.75f) {
        if (activeTrackId == trackId && isPlayingMusic) {
            trackVolume = volume.coerceIn(0f, 1f)
            isMusicPaused = false
            return
        }
        stopMusic()
        val track = MusicCatalog.find(trackId)
        if (track.notesHz.isEmpty()) return
        activeTrackId = trackId
        trackVolume = volume.coerceIn(0f, 1f)
        isPlayingMusic = true
        isMusicPaused = false

        musicJob = scope.launch {
            var idx = 0
            var fadeMultiplier = 0.25f
            while (isActive && isPlayingMusic) {
                if (!isMuted && !isMusicPaused) {
                    val effectiveVol = masterVolume * musicVolumePref * trackVolume * fadeMultiplier
                    if (effectiveVol > 0.01f) {
                        val freq = track.notesHz[idx % track.notesHz.size]
                        playSynthesizedTone(
                            freqHz = freq,
                            durationMs = (track.tempoMs * 0.90).toInt().coerceAtLeast(180),
                            amplitude = effectiveVol * 0.24f,
                            harmonicRatio = 0.20
                        )
                    }
                    fadeMultiplier = min(1f, fadeMultiplier + 0.18f)
                }
                idx++
                delay(track.tempoMs)
            }
        }
    }

    fun stopMusic() {
        isPlayingMusic = false
        isMusicPaused = false
        activeTrackId = "none"
        musicJob?.cancel()
        musicJob = null
    }

    private fun sfxAmp(base: Float): Float {
        if (isMuted) return 0f
        return (base * masterVolume * sfxVolumePref).coerceIn(0f, 0.55f)
    }

    fun playButtonClick() {
        val amp = sfxAmp(0.18f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(660.0, 45, amp, harmonicRatio = 0.08)
        }
    }

    fun playPopSound() {
        val amp = sfxAmp(0.28f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(740.0, 75, amp, harmonicRatio = 0.22)
        }
    }

    fun playCardFlip() {
        val amp = sfxAmp(0.20f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(480.0, 55, amp, harmonicRatio = 0.12)
        }
    }

    fun playCardMatch() {
        val amp = sfxAmp(0.28f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(587.33, 90, amp)
            delay(65)
            playSynthesizedTone(880.0, 145, amp * 1.05f)
        }
    }

    fun playCardMismatch() {
        val amp = sfxAmp(0.22f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(311.13, 95, amp, harmonicRatio = 0.15)
            delay(75)
            playSynthesizedTone(246.94, 130, amp * 0.9f, harmonicRatio = 0.15)
        }
    }

    fun playScratchTick() {
        val now = System.currentTimeMillis()
        if (now - lastScratchAtMs < 130L) return
        lastScratchAtMs = now
        val amp = sfxAmp(0.12f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(980.0, 35, amp, harmonicRatio = 0.10)
        }
    }

    fun playWheelTick() {
        val now = System.currentTimeMillis()
        if (now - lastWheelTickAtMs < 90L) return
        lastWheelTickAtMs = now
        val amp = sfxAmp(0.18f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(820.0, 38, amp, harmonicRatio = 0.15)
        }
    }

    fun playBoxShake() {
        val amp = sfxAmp(0.24f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(330.0, 75, amp, harmonicRatio = 0.18)
            delay(85)
            playSynthesizedTone(392.0, 85, amp, harmonicRatio = 0.18)
        }
    }

    fun playBoxOpen() {
        val amp = sfxAmp(0.30f)
        if (amp <= 0.005f) return
        scope.launch {
            listOf(440.0, 554.37, 659.25, 880.0, 1108.73).forEach { freq ->
                playSynthesizedTone(freq, 130, amp, harmonicRatio = 0.25)
                delay(75)
            }
        }
    }

    fun playEnvelopeOpen() {
        val amp = sfxAmp(0.26f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(523.25, 95, amp)
            delay(75)
            playSynthesizedTone(659.25, 110, amp)
            delay(75)
            playSynthesizedTone(783.99, 150, amp)
        }
    }

    fun playSparkleChime() {
        val amp = sfxAmp(0.26f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(587.33, 95, amp)
            delay(65)
            playSynthesizedTone(880.0, 140, amp)
        }
    }

    fun playSuccessFanfare() {
        val amp = sfxAmp(0.30f)
        if (amp <= 0.005f) return
        scope.launch {
            listOf(523.25, 659.25, 783.99, 1046.50).forEach { freq ->
                playSynthesizedTone(freq, 130, amp)
                delay(90)
            }
        }
    }

    fun playGameFailure() {
        val amp = sfxAmp(0.24f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(392.0, 110, amp, harmonicRatio = 0.12)
            delay(95)
            playSynthesizedTone(329.63, 130, amp, harmonicRatio = 0.12)
            delay(95)
            playSynthesizedTone(261.63, 180, amp * 0.9f, harmonicRatio = 0.12)
        }
    }

    fun playReplySent() {
        val amp = sfxAmp(0.28f)
        if (amp <= 0.005f) return
        scope.launch {
            playSynthesizedTone(659.25, 90, amp)
            delay(70)
            playSynthesizedTone(783.99, 90, amp)
            delay(70)
            playSynthesizedTone(1046.50, 170, amp)
        }
    }

    fun release() {
        stopMusic()
    }

    private fun playSynthesizedTone(
        freqHz: Double,
        durationMs: Int,
        amplitude: Float,
        harmonicRatio: Double = 0.25
    ) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs / 1000).coerceAtLeast(1)
        val attackSamples = (sampleRate * 0.012).toInt().coerceAtLeast(1) // 12ms soft fade-in
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decay = exp(-4.0 * t / (durationMs / 1000.0))
            val envelope = attack * decay
            val wave = sin(2.0 * PI * freqHz * t) + harmonicRatio * sin(4.0 * PI * freqHz * t)
            val sample = (wave * envelope * amplitude * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }

        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 18L)
            track.stop()
            track.release()
        }
    }
}

class VoiceNoteHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var startedAtMillis: Long = 0L

    fun startRecording(): Boolean {
        stopRecording()
        return runCatching {
            val dir = File(context.cacheDir, "voice").apply { mkdirs() }
            val outFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = outFile
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(24000)
                setAudioSamplingRate(16000)
                setMaxDuration(45000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            startedAtMillis = System.currentTimeMillis()
            true
        }.getOrElse {
            recorder = null
            false
        }
    }

    fun stopRecording(): Pair<String?, Long> {
        val durationSec = ((System.currentTimeMillis() - startedAtMillis) / 1000L).coerceIn(1L, 45L)
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        val file = currentFile ?: return null to 0L
        if (!file.exists() || file.length() == 0L) return null to 0L
        val bytes = file.readBytes()
        if (bytes.size > 175000) {
            return Base64.encodeToString(bytes.copyOf(175000), Base64.NO_WRAP) to durationSec
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP) to durationSec
    }

    fun playBase64Audio(base64: String, onCompleted: () -> Unit) {
        stopPlayback()
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val dir = File(context.cacheDir, "voice").apply { mkdirs() }
            val tempFile = File(dir, "play_voice.m4a")
            tempFile.writeBytes(bytes)
            val mp = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onCompleted()
                }
                start()
            }
            player = mp
        }.onFailure {
            onCompleted()
        }
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}

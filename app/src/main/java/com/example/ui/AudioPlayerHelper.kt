package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioPlayerHelper {
    private const val TAG = "AudioPlayerHelper"
    private const val SAMPLE_RATE = 44100
    private var droneJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Chime sound played upon task completion.
     * Synthesizes a beautiful major arpeggio (C5 -> E5 -> G5 -> C6) with exponential volume decay.
     */
    fun playCompletionChime() {
        scope.launch {
            try {
                val freqs = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
                val durationMs = 150
                for (freq in freqs) {
                    playTone(freq, durationMs)
                    delay(30)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play completion chime", e)
            }
        }
    }

    /**
     * Zen Bell sound played on timer transitions.
     * Synthesizes a deep resonant bowl sound.
     */
    fun playZenBell() {
        scope.launch {
            try {
                playTone(220f, 600, decay = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play Zen Bell", e)
            }
        }
    }

    /**
     * Synthesizes a simple sinusoidal tone at a specific frequency and duration.
     */
    private fun playTone(frequency: Float, durationMs: Int, decay: Boolean = true) {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val sample = DoubleArray(numSamples)
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Tone synthesis
            var envelope = 1.0
            if (decay) {
                envelope = 1.0 - (i.toDouble() / numSamples) // Linear decay
            }
            sample[i] = sin(2 * Math.PI * frequency * t) * envelope
        }

        var idx = 0
        for (dVal in sample) {
            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = valShort
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.size * 2,
                AudioTrack.MODE_STATIC
            )
        }

        track.write(generatedSnd, 0, generatedSnd.size)
        track.play()
    }

    /**
     * Starts playing a highly pleasant Zen Binaural Study drone in a separate thread.
     * Combines deep soft tones to mask background distractions (analogous to Lo-Fi synth).
     */
    fun startAmbientDrone(volumeMultiplier: Float = 0.5f) {
        if (droneJob != null) return // Already playing

        droneJob = scope.launch {
            // Setup AudioTrack for continuous streaming
            val bufferSize = SAMPLE_RATE * 2 // 1-second chunks
            val writeBuffer = ShortArray(bufferSize)

            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(bufferSize * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf.coerceAtLeast(bufferSize * 2),
                    AudioTrack.MODE_STREAM
                )
            }

            try {
                audioTrack?.play()
                var phase1 = 0.0
                var phase2 = 0.0
                val freq1 = 110.0 // A2 anchor note
                val freq2 = 165.0 // E3 perfect fifth warmth note

                while (isActive) {
                    for (i in 0 until bufferSize) {
                        t1 += (freq1 / SAMPLE_RATE)
                        t2 += (freq2 / SAMPLE_RATE)

                        // Sine oscillators
                        val sample1 = sin(2.0 * Math.PI * phase1)
                        val sample2 = sin(2.0 * Math.PI * phase2)
                        
                        // Soft chord synthesis with volume scale
                        val mixed = (sample1 * 0.5 + sample2 * 0.3) * volumeMultiplier
                        writeBuffer[i] = (mixed * 32767).toInt().coerceIn(-32768, 32767).toShort()

                        phase1 += freq1 / SAMPLE_RATE
                        if (phase1 > 1.0) phase1 -= 1.0
                        phase2 += freq2 / SAMPLE_RATE
                        if (phase2 > 1.0) phase2 -= 1.0
                    }
                    audioTrack?.write(writeBuffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Drone track stopped abnormally", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) { /* ignore */ }
                audioTrack = null
            }
        }
    }

    // Secondary phase vars for thread state
    private var t1 = 0.0
    private var t2 = 0.0

    /**
     * Stops the background focus music drone.
     */
    fun stopAmbientDrone() {
        droneJob?.cancel()
        droneJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) { /* ignore */ }
        audioTrack = null
    }

    /**
     * Modifies the drone output volume.
     */
    fun setVolume(volume: Float) {
        try {
            audioTrack?.setVolume(volume.coerceIn(0.0f, 1.0f))
        } catch (e: Exception) { /* ignore */ }
    }
}

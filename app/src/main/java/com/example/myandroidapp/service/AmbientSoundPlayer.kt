package com.example.myandroidapp.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri

/**
 * Manages ambient sound playback (looping) for the Focus Mode timer.
 *
 * Usage:
 *  AmbientSoundPlayer.play(context, "Deep Focus") // start ambient sound
 *  AmbientSoundPlayer.stop()                        // stop ambient sound
 *  AmbientSoundPlayer.setVolume(0.8f)               // adjust volume
 */
object AmbientSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: String = "Silence"

    // Prefer bundled tracks from res/raw; fallback keeps playback working until assets are added.
    private fun resolveSoundUri(context: Context, soundName: String): Uri? {
        val rawName = when (soundName) {
            "Deep Focus" -> "deep_focus"
            "Calm Piano" -> "calm_piano"
            "White Noise" -> "white_noise"
            "Lo-Fi Beats" -> "lofi_beats"
            else -> null
        }

        if (rawName != null) {
            val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
            if (resId != 0) {
                return "android.resource://${context.packageName}/$resId".toUri()
            }
        }

        // Backward-compatible fallback when raw assets are not yet bundled.
        return when (soundName) {
            "Deep Focus" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            "Calm Piano" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            "White Noise" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "Lo-Fi Beats" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            else -> null
        }
    }

    /**
     * Play an ambient sound by name. Stops the previous sound first.
     * Pass "Silence" to stop all sounds.
     */
    fun play(context: Context, soundName: String) {
        stop()
        currentSound = soundName

        if (soundName == "Silence") return

        val soundUri = resolveSoundUri(context, soundName) ?: return

        mediaPlayer = MediaPlayer().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes)
            setDataSource(context, soundUri)
            isLooping = true
            setOnPreparedListener { start() }
            setOnErrorListener { _, _, _ -> false }
            prepareAsync() // Non-blocking
        }
    }

    /**
     * Stop the currently playing sound and release MediaPlayer resources.
     */
    fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        currentSound = "Silence"
    }

    /**
     * Set playback volume (0.0 to 1.0).
     */
    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    /**
     * Returns the name of the currently playing sound.
     */
    fun getCurrentSound(): String = currentSound

    /**
     * Returns true if audio is currently playing.
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}

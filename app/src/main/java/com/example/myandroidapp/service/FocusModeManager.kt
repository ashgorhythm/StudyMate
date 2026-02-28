package com.example.myandroidapp.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build

/**
 * Handles DND (Do Not Disturb) mode activation for Focus Mode.
 * Uses NotificationManager policy access, which requires the user to grant
 * notification policy access via system settings.
 */
object FocusModeManager {

    /**
     * Check if the app has notification policy access (needed for DND).
     */
    fun hasNotificationPolicyAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    /**
     * Enable DND mode - silences all notifications except allowed contacts.
     * @param allowPriorityCalls If true, allows priority calls (starred contacts or repeated callers).
     */
    fun enableDndMode(context: Context, allowPriorityCalls: Boolean = true) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!nm.isNotificationPolicyAccessGranted) return

        // Set DND to priority-only if allowing some calls, or total silence otherwise
        if (allowPriorityCalls) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

            // Configure priority to allow starred contacts and repeated callers
            val policy = NotificationManager.Policy(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS,
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY
            )
            nm.notificationPolicy = policy
        } else {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    /**
     * Disable DND mode - restores normal notification behavior.
     */
    fun disableDndMode(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    /**
     * Set ringer mode to silent for Focus mode.
     */
    fun silencePhone(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
    }

    /**
     * Restore ringer mode to normal after Focus mode.
     */
    fun restoreRinger(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }
}

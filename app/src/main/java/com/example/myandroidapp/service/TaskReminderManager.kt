package com.example.myandroidapp.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myandroidapp.MainActivity
import com.example.myandroidapp.R

/**
 * Handles scheduling and firing of task due-date reminder notifications.
 */
object TaskReminderManager {

    const val CHANNEL_ID = "task_reminder_channel"
    const val CHANNEL_NAME = "Task Reminders"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_TASK_ID = "task_id"
    const val NOTIFICATION_REQUEST_BASE = 7000

    /**
     * Creates the notification channel (required for Android 8+).
     * Call this once from Application.onCreate or MainActivity.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Reminders for upcoming study tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule a reminder notification for a task.
     * The reminder fires 1 hour before the due date (or immediately if < 1 hour away).
     */
    fun scheduleReminder(context: Context, taskId: Long, taskTitle: String, dueDateMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Fire 1 hour before due date
        val reminderTime = dueDateMillis - (60 * 60 * 1000L)
        val triggerTime = maxOf(reminderTime, System.currentTimeMillis() + 5_000L) // at least 5s from now

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_ID, taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (NOTIFICATION_REQUEST_BASE + taskId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fall back to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    /**
     * Cancel a previously scheduled reminder.
     */
    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (NOTIFICATION_REQUEST_BASE + taskId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Show an immediate notification for a task.
     */
    fun showNotification(context: Context, taskId: Long, taskTitle: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 Task Due Soon!")
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"$taskTitle\" is due in 1 hour. Time to focus!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        notificationManager.notify((NOTIFICATION_REQUEST_BASE + taskId).toInt(), notification)
    }
}

/**
 * BroadcastReceiver that fires when an alarm triggers and shows the notification.
 */
class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra(TaskReminderManager.EXTRA_TASK_TITLE) ?: "Task"
        val taskId = intent.getLongExtra(TaskReminderManager.EXTRA_TASK_ID, 0L)
        TaskReminderManager.showNotification(context, taskId, taskTitle)
    }
}

package com.planwell.app.data.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.planwell.app.R
import com.planwell.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        ensureTaskChannel()
        ensureCallChannel()
    }

    private fun ensureTaskChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for Plan Well tasks"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun ensureCallChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            "Scheduled calls",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for scheduled calls"
            setBypassDnd(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showTaskReminder(taskId: Long, title: String, body: String? = null) {
        ensureTaskChannel()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body ?: "Tap to open task")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }

    fun showCallReminder(
        callId: Long,
        displayName: String,
        phoneNumber: String,
        notes: String? = null,
    ) {
        ensureCallChannel()
        val title = context.getString(R.string.scheduled_call)
        val body = buildString {
            append(displayName)
            if (phoneNumber.isNotBlank()) {
                append(" · ")
                append(phoneNumber)
            }
            if (!notes.isNullOrBlank()) {
                append('\n')
                append(notes)
            }
        }

        val callNow = CallReminderIntents.callNowPendingIntent(
            context, callId, phoneNumber, displayName,
        )
        val snooze = CallReminderIntents.snoozePendingIntent(
            context, callId, phoneNumber, displayName,
        )
        val fullScreen = CallReminderIntents.fullScreenPendingIntent(
            context, callId, phoneNumber, displayName,
        )
        val content = CallReminderIntents.contentPendingIntent(context, callId)

        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(content)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, context.getString(R.string.call_now), callNow)
            .addAction(0, context.getString(R.string.snooze_15m), snooze)
            .build()

        NotificationManagerCompat.from(context)
            .notify(callNotificationId(callId), notification)
    }

    fun cancelCallReminder(callId: Long) {
        NotificationManagerCompat.from(context).cancel(callNotificationId(callId))
    }

    companion object {
        const val CHANNEL_ID = "plan_well_reminders"
        const val CALL_CHANNEL_ID = "plan_well_calls"

        fun callNotificationId(callId: Long): Int =
            CallReminderIntents.requestCode(callId, kind = 5)
    }
}

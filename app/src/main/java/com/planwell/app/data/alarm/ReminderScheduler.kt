package com.planwell.app.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.planwell.app.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
) {
    fun schedule(reminder: Reminder) {
        val pendingIntent = buildPendingIntent(reminder.id, reminder.taskId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAtMs,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES / 4,
                pendingIntent,
            )
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.triggerAtMs,
            pendingIntent,
        )
    }

    fun cancel(reminderId: Long) {
        alarmManager.cancel(buildPendingIntent(reminderId, taskId = 0))
    }

    private fun buildPendingIntent(reminderId: Long, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

package com.planwell.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.planwell.app.data.local.dao.TaskDao
import com.planwell.app.data.worker.RescheduleReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (taskId < 0 || reminderId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getTaskById(taskId)
                val title = task?.title ?: "Plan Well reminder"
                notificationHelper.showTaskReminder(
                    taskId = taskId,
                    title = title,
                    body = task?.description,
                )

                val work = OneTimeWorkRequestBuilder<RescheduleReminderWorker>()
                    .setInputData(workDataOf(RescheduleReminderWorker.KEY_REMINDER_ID to reminderId))
                    .build()
                WorkManager.getInstance(context).enqueue(work)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}

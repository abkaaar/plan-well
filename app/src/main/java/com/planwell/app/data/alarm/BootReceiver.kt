package com.planwell.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.planwell.app.data.worker.RestoreRemindersWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val work = OneTimeWorkRequestBuilder<RestoreRemindersWorker>().build()
        WorkManager.getInstance(context).enqueue(work)
    }
}

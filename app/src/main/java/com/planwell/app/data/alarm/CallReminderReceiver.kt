package com.planwell.app.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.planwell.app.domain.repository.CallRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var callRepository: CallRepository

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getLongExtra(CallReminderIntents.EXTRA_CALL_ID, -1L)
        if (callId < 0) return

        val phone = intent.getStringExtra(CallReminderIntents.EXTRA_PHONE).orEmpty()
        val displayName = intent.getStringExtra(CallReminderIntents.EXTRA_DISPLAY_NAME).orEmpty()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    CallReminderIntents.ACTION_SNOOZE -> {
                        val triggerAt = CallReminderIntents.snoozeTriggerAt()
                        callRepository.snooze(callId, triggerAt)
                        notificationHelper.cancelCallReminder(callId)
                    }
                    else -> {
                        val call = callRepository.getById(callId)
                        notificationHelper.showCallReminder(
                            callId = callId,
                            displayName = call?.displayName?.takeIf { it.isNotBlank() }
                                ?: displayName.ifBlank { "Scheduled call" },
                            phoneNumber = call?.phoneNumber?.takeIf { it.isNotBlank() } ?: phone,
                            notes = call?.notes,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
